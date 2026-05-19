package ro.autobrand.proba.service;

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
}