package digital.pragmatech.tour_de_jug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual utility test to scrape Java User Group data from two public sources
 * and produce a merged jugs.yaml file.
 *
 * Sources:
 * - https://dev.java/community/jugs/ (HTML table with name, city, country, status, URL)
 * - https://github.com/World-Wide-JUGs/GlobalWWJugs (markdown files with coordinates)
 *
 * Run manually: RUN_SCRAPER=true ./mvnw test -Dtest=JugDataScraperTest
 * With GitHub coordinates: RUN_SCRAPER=true GITHUB_TOKEN=ghp_xxx ./mvnw test -Dtest=JugDataScraperTest
 *
 * Set GITHUB_TOKEN env var to avoid rate limiting when fetching GlobalWWJugs data.
 */
@EnabledIfEnvironmentVariable(named = "RUN_SCRAPER", matches = "true")
class JugDataScraperTest {

    private static final String DEV_JAVA_URL = "https://dev.java/community/jugs/";
    private static final String GITHUB_TREE_URL = "https://api.github.com/repos/World-Wide-JUGs/GlobalWWJugs/git/trees/master?recursive=1";

    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("(.+?)\\s*\\([A-Z]{2}\\)\\s*$");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*");
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("---\\s*\\n(.*?)\\n---", Pattern.DOTALL);

    private static final Map<String, String> COUNTRY_NORMALIZATION = Map.of(
            "United States", "USA",
            "United Kingdom", "UK",
            "Korea, Republic of", "South Korea",
            "Russian Federation", "Russia"
    );

    record DevJavaJug(String name, String city, String state, String country, String status, String homepageUrl) {}
    record GitHubJug(String name, String country, String website, String twitter,
                     Double latitude, Double longitude, Integer foundedYear) {}
    record MergedJug(String slug, String name, String city, String country,
                     String homepageUrl, Double latitude, Double longitude, Integer establishedYear,
                     boolean active) {}

    @Test
    void scrapeAndMergeJugData() throws Exception {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // 1. Scrape dev.java
        var devJavaJugs = scrapeDevJava(httpClient);
        System.out.printf("Fetched %d active JUGs from dev.java%n", devJavaJugs.size());

        // 2. Scrape GlobalWWJugs (if token available)
        var githubJugs = scrapeGitHubJugs(httpClient);
        System.out.printf("Fetched %d JUGs from GlobalWWJugs%n", githubJugs.size());

        // 3. Merge
        var merged = mergeJugs(devJavaJugs, githubJugs);
        long withCoordsBefore = merged.stream().filter(j -> j.latitude() != null).count();
        System.out.printf("Merged result: %d JUGs (%d with coordinates)%n", merged.size(), withCoordsBefore);

        // 4. Geocode JUGs missing coordinates using city center
        merged = geocodeMissingCoordinates(merged, httpClient);
        long withCoordsAfter = merged.stream().filter(j -> j.latitude() != null).count();
        System.out.printf("After geocoding: %d with coordinates (added %d)%n", withCoordsAfter, withCoordsAfter - withCoordsBefore);

        // 5. Write YAML
        var outputPath = Path.of("target/scraped-jugs.yaml");
        writeYaml(merged, outputPath);
        System.out.println("Written to " + outputPath.toAbsolutePath());

        // 6. Sanity checks
        assertThat(merged).hasSizeGreaterThan(200);
        assertThat(Files.exists(outputPath)).isTrue();
    }

    List<DevJavaJug> scrapeDevJava(HttpClient httpClient) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(DEV_JAVA_URL))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("dev.java response status").isEqualTo(200);

        Document doc = Jsoup.parse(response.body());
        Elements tables = doc.select("table");
        assertThat(tables).as("dev.java page should contain a table").isNotEmpty();

        Element table = tables.first();
        Elements rows = table.select("tr");

        var jugs = new ArrayList<DevJavaJug>();
        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() < 5) continue;

            String name = cells.get(0).text().trim();
            String homepageUrl = null;
            Element link = cells.get(0).selectFirst("a");
            if (link != null) {
                homepageUrl = link.attr("href").trim();
                if (!homepageUrl.isEmpty() && !homepageUrl.startsWith("http")) {
                    homepageUrl = "https://" + homepageUrl;
                }
            }

            String city = cells.get(1).text().trim();
            String state = cells.get(2).text().trim();
            String country = normalizeCountry(cells.get(3).text().trim());
            String status = cells.get(4).text().trim();

            if (name.isEmpty()) continue;

            jugs.add(new DevJavaJug(name, emptyToNull(city), emptyToNull(state), country, status, emptyToNull(homepageUrl)));
        }

        return jugs;
    }

    List<GitHubJug> scrapeGitHubJugs(HttpClient httpClient) throws Exception {
        String githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken == null || githubToken.isBlank()) {
            System.out.println("WARNING: GITHUB_TOKEN not set. Skipping GlobalWWJugs (coordinates will be missing).");
            System.out.println("Set a GitHub personal access token (no scopes needed) to fetch coordinate data.");
            return List.of();
        }

        var objectMapper = new ObjectMapper();

        // Fetch the full tree in a single API call
        var treeRequest = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_TREE_URL))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        var treeResponse = httpClient.send(treeRequest, HttpResponse.BodyHandlers.ofString());
        if (treeResponse.statusCode() != 200) {
            System.out.println("WARNING: GitHub tree API returned " + treeResponse.statusCode() + ". Skipping GlobalWWJugs.");
            return List.of();
        }

        JsonNode treeRoot = objectMapper.readTree(treeResponse.body());
        JsonNode treeEntries = treeRoot.get("tree");

        // Find all _jugs/*.md blob entries
        var jugBlobs = new ArrayList<Map.Entry<String, String>>();
        for (JsonNode entry : treeEntries) {
            String path = entry.get("path").asText();
            if (path.startsWith("_jugs/") && path.endsWith(".md") && "blob".equals(entry.get("type").asText())) {
                jugBlobs.add(Map.entry(path, entry.get("url").asText()));
            }
        }

        System.out.printf("Found %d JUG files in GlobalWWJugs repo%n", jugBlobs.size());

        var jugs = new ArrayList<GitHubJug>();
        for (var blob : jugBlobs) {
            try {
                var blobRequest = HttpRequest.newBuilder()
                        .uri(URI.create(blob.getValue()))
                        .header("Authorization", "Bearer " + githubToken)
                        .header("Accept", "application/vnd.github.v3+json")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                var blobResponse = httpClient.send(blobRequest, HttpResponse.BodyHandlers.ofString());
                if (blobResponse.statusCode() != 200) {
                    System.out.println("WARNING: Failed to fetch " + blob.getKey() + " (HTTP " + blobResponse.statusCode() + ")");
                    continue;
                }

                JsonNode blobNode = objectMapper.readTree(blobResponse.body());
                String content = blobNode.get("content").asText();
                // GitHub returns base64 with newlines
                content = new String(Base64.getMimeDecoder().decode(content));

                var jug = parseGitHubJugFrontmatter(content);
                if (jug != null) {
                    jugs.add(jug);
                }
            } catch (Exception e) {
                System.out.println("WARNING: Error processing " + blob.getKey() + ": " + e.getMessage());
            }
        }

        return jugs;
    }

    GitHubJug parseGitHubJugFrontmatter(String content) {
        Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(content);
        if (!frontmatterMatcher.find()) return null;

        String frontmatter = frontmatterMatcher.group(1);
        Map<String, String> fields = new LinkedHashMap<>();
        for (String line : frontmatter.split("\n")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                // Remove surrounding quotes
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                fields.put(key, value);
            }
        }

        String name = fields.get("name");
        if (name == null || name.isBlank()) return null;

        String country = fields.get("country");
        String website = fields.get("website");
        String twitter = fields.get("twitter");

        Double latitude = null;
        Double longitude = null;
        String location = fields.get("location");
        if (location != null) {
            Matcher locationMatcher = LOCATION_PATTERN.matcher(location);
            if (locationMatcher.matches()) {
                latitude = Double.parseDouble(locationMatcher.group(1));
                longitude = Double.parseDouble(locationMatcher.group(2));
            }
        }

        Integer foundedYear = null;
        String foundedDate = fields.get("founded_date");
        if (foundedDate != null && foundedDate.length() >= 4) {
            try {
                foundedYear = Integer.parseInt(foundedDate.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }

        return new GitHubJug(name, country, emptyToNull(website), emptyToNull(twitter),
                latitude, longitude, foundedYear);
    }

    List<MergedJug> mergeJugs(List<DevJavaJug> devJavaJugs, List<GitHubJug> githubJugs) {
        // Build lookup by normalized name
        Map<String, GitHubJug> githubByNormalizedName = new HashMap<>();
        for (var jug : githubJugs) {
            githubByNormalizedName.put(normalizeName(jug.name()), jug);
        }

        Set<String> matchedGithubNames = new HashSet<>();
        var merged = new ArrayList<MergedJug>();
        var seenSlugs = new HashSet<String>();

        // Process dev.java JUGs as primary source
        for (var devJug : devJavaJugs) {
            String normalizedName = normalizeName(devJug.name());
            GitHubJug githubMatch = githubByNormalizedName.get(normalizedName);

            if (githubMatch != null) {
                matchedGithubNames.add(normalizeName(githubMatch.name()));
            }

            String homepageUrl = devJug.homepageUrl();
            if (homepageUrl == null && githubMatch != null) {
                homepageUrl = githubMatch.website();
            }

            Double latitude = githubMatch != null ? githubMatch.latitude() : null;
            Double longitude = githubMatch != null ? githubMatch.longitude() : null;
            Integer establishedYear = githubMatch != null ? githubMatch.foundedYear() : null;

            String slug = generateSlug(devJug.name());
            if (seenSlugs.contains(slug)) {
                // Append city to disambiguate
                if (devJug.city() != null) {
                    slug = generateSlug(devJug.name() + " " + devJug.city());
                }
            }
            if (seenSlugs.contains(slug)) continue; // skip true duplicates
            seenSlugs.add(slug);

            boolean active = "Active".equalsIgnoreCase(devJug.status());
            merged.add(new MergedJug(slug, devJug.name(), devJug.city(), devJug.country(),
                    homepageUrl, latitude, longitude, establishedYear, active));
        }

        // Append GitHub-only JUGs not found in dev.java
        for (var githubJug : githubJugs) {
            if (matchedGithubNames.contains(normalizeName(githubJug.name()))) continue;

            String slug = generateSlug(githubJug.name());
            if (seenSlugs.contains(slug)) continue;
            seenSlugs.add(slug);

            String country = githubJug.country() != null ? normalizeCountry(githubJug.country()) : null;

            merged.add(new MergedJug(slug, githubJug.name(), null, country,
                    githubJug.website(), githubJug.latitude(), githubJug.longitude(), githubJug.foundedYear(), true));
        }

        // Sort by name
        merged.sort(Comparator.comparing(MergedJug::name, String.CASE_INSENSITIVE_ORDER));
        return merged;
    }

    List<MergedJug> geocodeMissingCoordinates(List<MergedJug> jugs, HttpClient httpClient) throws Exception {
        var objectMapper = new ObjectMapper();
        // Cache city+country -> coordinates to avoid duplicate lookups
        var geocodeCache = new HashMap<String, double[]>();
        var result = new ArrayList<MergedJug>();
        int geocoded = 0;
        int failed = 0;

        for (var jug : jugs) {
            if (jug.latitude() != null && jug.longitude() != null) {
                result.add(jug);
                continue;
            }

            // Build a lookup key from city + country
            String city = jug.city();
            String country = jug.country();
            if (city == null && country == null) {
                result.add(jug);
                continue;
            }

            String cacheKey = (city != null ? city : "") + "|" + (country != null ? country : "");
            double[] cached = geocodeCache.get(cacheKey);

            if (cached == null) {
                // Try multiple query strategies with Nominatim
                List<String> queries = new ArrayList<>();
                if (city != null && country != null) {
                    queries.add("city=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                            + "&country=" + URLEncoder.encode(country, StandardCharsets.UTF_8));
                    queries.add("q=" + URLEncoder.encode(city + ", " + country, StandardCharsets.UTF_8));
                    queries.add("q=" + URLEncoder.encode(city, StandardCharsets.UTF_8));
                } else if (city != null) {
                    queries.add("q=" + URLEncoder.encode(city, StandardCharsets.UTF_8));
                } else {
                    queries.add("q=" + URLEncoder.encode(country, StandardCharsets.UTF_8));
                }

                for (String queryParams : queries) {
                    // Nominatim requires max 1 request per second
                    Thread.sleep(1100);

                    String url = "https://nominatim.openstreetmap.org/search?" + queryParams
                            + "&format=json&limit=1";

                    try {
                        var request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("User-Agent", "TourDeJUG-Scraper/1.0")
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build();

                        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 200) {
                            JsonNode results = objectMapper.readTree(response.body());
                            if (results.isArray() && !results.isEmpty()) {
                                double lat = results.get(0).get("lat").asDouble();
                                double lon = results.get(0).get("lon").asDouble();
                                cached = new double[]{lat, lon};
                                geocodeCache.put(cacheKey, cached);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("WARNING: Geocoding failed for '" + queryParams + "': " + e.getMessage());
                    }
                }
            }

            if (cached != null) {
                result.add(new MergedJug(jug.slug(), jug.name(), jug.city(), jug.country(),
                        jug.homepageUrl(), cached[0], cached[1], jug.establishedYear(), jug.active()));
                geocoded++;
            } else {
                result.add(jug);
                failed++;
                System.out.println("WARNING: Could not geocode: " + jug.name() + " (" + cacheKey + ")");
            }
        }

        System.out.printf("Geocoding: %d succeeded, %d failed, %d cache hits%n",
                geocoded, failed, geocoded - geocodeCache.size());
        return result;
    }

    void writeYaml(List<MergedJug> jugs, Path outputPath) throws Exception {
        var yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        var yamlMapper = new ObjectMapper(yamlFactory);
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        List<Map<String, Object>> jugList = jugs.stream()
                .map(jug -> {
                    var map = new LinkedHashMap<String, Object>();
                    map.put("slug", jug.slug());
                    map.put("name", jug.name());
                    if (jug.city() != null) map.put("city", jug.city());
                    map.put("country", jug.country());
                    if (jug.homepageUrl() != null) map.put("homepage_url", jug.homepageUrl());
                    if (jug.latitude() != null) map.put("latitude", jug.latitude());
                    if (jug.longitude() != null) map.put("longitude", jug.longitude());
                    if (jug.establishedYear() != null) map.put("established_year", jug.establishedYear());
                    if (!jug.active()) map.put("active", false);
                    return (Map<String, Object>) map;
                })
                .toList();

        var root = new LinkedHashMap<String, Object>();
        root.put("jugs", jugList);

        Files.createDirectories(outputPath.getParent());
        yamlMapper.writeValue(outputPath.toFile(), root);
    }

    static String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    static String normalizeName(String name) {
        return name.toLowerCase()
                .replaceAll("\\bjava\\b", "")
                .replaceAll("\\buser\\b", "")
                .replaceAll("\\bgroups?\\b", "")
                .replaceAll("\\bjug\\b", "")
                .replaceAll("\\bcommunity\\b", "")
                .replaceAll("\\busers\\b", "")
                .replaceAll("\\bmeetup\\b", "")
                .replaceAll("\\bassociation\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    static String normalizeCountry(String country) {
        if (country == null || country.isBlank()) return null;

        // Strip country code in parentheses: "United States (US)" -> "United States"
        Matcher matcher = COUNTRY_CODE_PATTERN.matcher(country);
        if (matcher.matches()) {
            country = matcher.group(1).trim();
        }

        return COUNTRY_NORMALIZATION.getOrDefault(country, country);
    }

    static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
