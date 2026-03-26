# Tour de JUG

Discover Java User Groups worldwide. Browse the interactive map, explore JUG details, and — if you're a speaker — track your conference talks with a public profile.

**Live at:** [tour-de-jug.pragmatech.digital](https://tour-de-jug.pragmatech.digital)

## Contributing a New Java User Group

All JUG data lives in [`src/main/resources/jugs.yaml`](src/main/resources/jugs.yaml). To add or update a JUG:

1. Fork this repository
2. Edit `src/main/resources/jugs.yaml` and add an entry:

```yaml
- slug: my-city-jug          # URL-safe identifier (lowercase, hyphens)
  name: My City JUG
  city: My City
  country: My Country
  homepage_url: https://mycityjug.org
  latitude: 48.8566           # decimal degrees
  longitude: 2.3522
  established_year: 2015
  # Optional fields:
  # social_url: https://twitter.com/mycityjug
  # meetup_url: https://meetup.com/mycityjug
  # description: A friendly community of Java developers in My City.
```

3. Open a pull request against `main`

The application syncs `jugs.yaml` on every startup — new entries are added, existing ones updated, and removed entries marked inactive.

## Running Locally

**Prerequisites:** Java 21, Docker

```bash
# Start the app (PostgreSQL starts automatically via Docker Compose)
./mvnw spring-boot:run

# Run all tests (unit + integration + E2E)
./mvnw verify
```

Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables for OAuth login, or leave defaults for anonymous browsing.

## License

[MIT](LICENSE)
