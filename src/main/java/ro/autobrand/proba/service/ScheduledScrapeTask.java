package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.scraping.cron-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ScheduledScrapeTask {

    private final ScrapingService scrapingService;

    @Scheduled(cron = "${app.scraping.cron:0 0 12-18 * * *}")
    public void scheduledRun() {
        log.info("Scheduled scrape triggered");
        try {
            scrapingService.runScrape();
        } catch (Exception e) {
            log.error("Scheduled scrape failed", e);
        }
    }
}