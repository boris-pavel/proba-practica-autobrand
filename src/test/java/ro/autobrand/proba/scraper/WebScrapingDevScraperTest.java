package ro.autobrand.proba.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.ScrapedProductDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebScrapingDevScraperTest {

    @Test
    void parses_products_from_fixture_html() throws IOException {
        Document doc;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/products-page.html")) {
            doc = Jsoup.parse(in, StandardCharsets.UTF_8.name(), "https://www.web-scraping.dev/");
        }

        WebScrapingDevScraper scraper = new WebScrapingDevScraper("https://test", "u", "p");
        List<ScrapedProductDto> products = scraper.parseProducts(doc);

        assertThat(products).isNotEmpty();
        ScrapedProductDto first = products.get(0);
        assertThat(first.getName()).isNotBlank();
        assertThat(first.getPrice()).isPositive();
        assertThat(first.getCurrency()).hasSize(3);
        assertThat(first.getImageUrl()).startsWith("http");
    }
}