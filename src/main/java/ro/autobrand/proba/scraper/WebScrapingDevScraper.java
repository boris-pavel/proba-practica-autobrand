package ro.autobrand.proba.scraper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.autobrand.proba.dto.ScrapedProductDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class WebScrapingDevScraper implements Scraper {

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([A-Z]{3}|\\$|€|£)?");

    private final String baseUrl;
    private final String username;
    private final String password;

    public WebScrapingDevScraper(
            @Value("${app.scraping.base-url:https://www.web-scraping.dev}") String baseUrl,
            @Value("${app.scraping.username:user123}") String username,
            @Value("${app.scraping.password:password}") String password
    ) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public List<ScrapedProductDto> scrape() {
        try {
            Map<String, String> cookies = login();
            Document doc = fetchProductsPage(cookies);
            return parseProducts(doc);
        } catch (IOException e) {
            log.error("Scraping failed", e);
            throw new ScrapingException("Failed to scrape products", e);
        }
    }

    Map<String, String> login() throws IOException {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

        // 1. GET /login — captează cookies de sesiune (CSRF, session_id, etc.)
        Connection.Response getRes = Jsoup.connect(baseUrl + "/login")
                .userAgent(userAgent)
                .method(Connection.Method.GET)
                .execute();
        log.debug("GET /login → {}, cookies: {}", getRes.statusCode(), getRes.cookies().keySet());

        // 2. POST /api/login cu cookies + credentials + User-Agent realistic
        Connection.Response postRes = Jsoup.connect(baseUrl + "/api/login")
                .userAgent(userAgent)
                .header("Origin", baseUrl)
                .header("Referer", baseUrl + "/login")
                .cookies(getRes.cookies())
                .data("username", username)
                .data("password", password)
                .method(Connection.Method.POST)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)           // ← NOU
                .execute();

        if (postRes.statusCode() >= 400) {
            throw new IOException("Login failed: HTTP " + postRes.statusCode()
                    + " body: " + postRes.body().substring(0, Math.min(200, postRes.body().length())));
        }
        log.info("Login succeeded ({}), cookies: {}", postRes.statusCode(), postRes.cookies().keySet());

        // Combinăm cookies din GET (pre-session) + POST (auth confirmat)
        Map<String, String> allCookies = new java.util.HashMap<>(getRes.cookies());
        allCookies.putAll(postRes.cookies());
        return allCookies;
    }

    Document fetchProductsPage(Map<String, String> cookies) throws IOException {
        return Jsoup.connect(baseUrl + "/products?category=consumables")
                .cookies(cookies)
                .get();
    }

    List<ScrapedProductDto> parseProducts(Document doc) {
        List<ScrapedProductDto> result = new ArrayList<>();
        for (Element card : doc.select(".product")) {
            Element link = card.selectFirst("a");
            String name = link != null ? link.text().trim() : null;
            String sourceUrl = link != null ? link.absUrl("href") : null;

            String desc = textOrEmpty(card, "div.short-description");
            String priceRaw = textOrEmpty(card, "div.price");
            Element img = card.selectFirst("img.img-thumbnail");
            String imageUrl = img != null ? img.absUrl("src") : null;

            PriceParsed parsed = parsePrice(priceRaw);
            if (parsed == null) {
                log.warn("Could not parse price '{}' for product '{}' — skipping", priceRaw, name);
                continue;
            }
            if (name == null || name.isBlank()) {
                log.warn("Product card with blank name — skipping");
                continue;
            }

            result.add(ScrapedProductDto.builder()
                    .name(name)
                    .description(desc)
                    .price(parsed.amount())
                    .currency(parsed.currency())
                    .imageUrl(imageUrl)
                    .sourceUrl(sourceUrl)
                    .build());
        }
        log.info("Parsed {} products", result.size());
        return result;
    }

    private String textOrEmpty(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el == null ? "" : el.text().trim();
    }

    private PriceParsed parsePrice(String raw) {
        Matcher m = PRICE_PATTERN.matcher(raw);
        if (!m.find()) return null;
        BigDecimal amount = new BigDecimal(m.group(1));
        String symbol = m.group(2);
        String currency = symbol == null ? "USD" : switch (symbol) {
            case "$" -> "USD";
            case "€" -> "EUR";
            case "£" -> "GBP";
            default -> symbol;
        };
        return new PriceParsed(amount, currency);
    }

    private record PriceParsed(BigDecimal amount, String currency) {}
}