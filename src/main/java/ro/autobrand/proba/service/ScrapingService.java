package ro.autobrand.proba.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.scraper.Scraper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final Scraper scraper;
    private final ProductService productService;

    public ProductService.UpsertResult runScrape() {
        log.info("Starting scrape run");
        List<ScrapedProductDto> scraped = scraper.scrape();
        ProductService.UpsertResult result = productService.upsertAll(scraped);
        log.info("Scrape run completed: {}", result);
        return result;
    }

    @Value("${app.scraping.run-on-startup:false}")
    private boolean scrapeOnStartup;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (scrapeOnStartup) {
            log.info("Running scrape on application startup");
            try {
                runScrape();
            } catch (Exception e) {
                log.error("Startup scrape failed", e);
            }
        }
    }
}