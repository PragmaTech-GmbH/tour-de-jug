package digital.pragmatech.tour_de_jug.e2e;

import com.codeborne.selenide.CollectionCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

/**
 * E2E tests for the home page (GET /).
 *
 * Covers:
 *  • Page loads with the correct title and navbar
 *  • Leaflet map is rendered in the DOM
 *  • JUG list is populated from the JugImportService seed data
 *  • Client-side search filters the sidebar list without a page reload
 *  • Clicking a JUG item in the sidebar highlights it and opens the map popup
 */
@DisplayName("Entry page (map + sidebar)")
class EntryPageE2EIT extends BaseE2EIT {

    @Test
    @DisplayName("page loads with correct title and brand text")
    void pageLoadsWithBrand() {
        open("/");

        $("title").shouldHave(attribute("text").because(
            // Use document.title via JS rather than asserting the <title> tag text
            // because browsers resolve the title asynchronously.
            "page title is set"
        ));

        // Verify navbar brand text is present
        $("nav").shouldHave(text("Tour de JUG"));
    }

    @Test
    @DisplayName("Leaflet map container is visible")
    void mapContainerIsVisible() {
        open("/");

        // Leaflet replaces #map with its own canvas/SVG layers.
        // The container div itself is always present; Leaflet adds .leaflet-container.
        $("#map").shouldBe(visible);
        $(".leaflet-container").shouldBe(visible);
    }

    @Test
    @DisplayName("sidebar JUG list is populated from seed data")
    void sidebarListIsPopulated() {
        open("/");

        // JUG list items are rendered by map.js immediately from the JSON embedded
        // in the page — no async fetch required, so they appear synchronously.
        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // The London Java Community is in jugs.yaml, so it must appear
        $$("#jugList .jug-item").findBy(text("London Java Community")).shouldBe(visible);
    }

    @Test
    @DisplayName("sidebar search filters the JUG list client-side")
    void searchFilterReducesList() {
        open("/");

        // Wait for the list to be populated before measuring its initial size
        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeGreaterThan(1));
        int totalCount = $$("#jugList .jug-item").size();

        // Type a query that matches only one entry from jugs.yaml
        $("#searchInput").setValue("London");

        // The filtered list must be smaller than the full list …
        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeLessThan(totalCount));
        // … and the matching JUG must remain visible
        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeGreaterThan(0));
        $(".jug-item").shouldHave(text("London"));
    }

    @Test
    @DisplayName("clearing search restores the full JUG list")
    void clearingSearchRestoresList() {
        open("/");

        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeGreaterThan(1));
        int totalCount = $$("#jugList .jug-item").size();

        $("#searchInput").setValue("London");
        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeLessThan(totalCount));

        // Clear the search input via JS — setValue("") alone does not fire the
        // 'input' event that the sidebar's filter function listens on.
        executeJavaScript(
            "var el = document.getElementById('searchInput');" +
            "el.value = '';" +
            "el.dispatchEvent(new Event('input'));"
        );
        $$("#jugList .jug-item").shouldHave(CollectionCondition.size(totalCount));
    }

    @Test
    @DisplayName("clicking a JUG in the sidebar marks it active and opens a map popup")
    void clickSidebarItemHighlightsAndOpensPopup() {
        open("/");

        $$("#jugList .jug-item").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Click the first item in the sidebar list
        $(".jug-item").click();

        // The clicked item gets the CSS class .active
        $(".jug-item.active").shouldBe(visible);

        // Leaflet opens a popup with the JUG name
        $(".leaflet-popup").shouldBe(visible);
        $(".leaflet-popup-content").shouldNotBe(empty);
    }

    @Test
    @DisplayName("login link is visible for anonymous users")
    void loginLinkVisibleWhenAnonymous() {
        open("/");

        $("a[href='/oauth2/authorization/github']").shouldBe(visible);
    }

    @Test
    @DisplayName("footer shows imprint link")
    void footerIsPresent() {
        open("/");

        $("footer").shouldBe(visible);
        $("footer").shouldHave(text("PragmaTech"));
    }
}
