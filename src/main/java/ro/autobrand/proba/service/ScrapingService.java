package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.model.ScrapeRun;
import ro.autobrand.proba.repository.ScrapeRunRepository;
import ro.autobrand.proba.scraper.Scraper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final Scraper scraper;
    private final ProductService productService;
    private final ExchangeRateService exchangeRateService;
    private final ScrapeRunRepository scrapeRunRepo;

    @Value("${app.scraping.run-on-startup:false}")
    private boolean scrapeOnStartup;

    public ProductService.UpsertResult runScrape() {
        log.info("Starting scrape run");
        ScrapeRun run = scrapeRunRepo.save(ScrapeRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build());
        try {
            List<ScrapedProductDto> scraped = scraper.scrape();
            ProductService.UpsertResult result = productService.upsertAll(scraped);

            exchangeRateService.ensureTodayRates();
            exchangeRateService.recomputeRon();

            run.setStatus("SUCCESS");
            run.setFinishedAt(LocalDateTime.now());
            run.setProductsTotal(result.total());
            run.setProductsNew(result.inserted());
            run.setProductsUpdated(result.updated());
            scrapeRunRepo.save(run);

            log.info("Scrape run completed: {}", result);
            return result;

        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setFinishedAt(LocalDateTime.now());
            run.setErrorMessage(e.getMessage());
            scrapeRunRepo.save(run);
            log.error("Scrape run failed", e);
            throw e;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
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