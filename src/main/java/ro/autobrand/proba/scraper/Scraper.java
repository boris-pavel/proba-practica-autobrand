package ro.autobrand.proba.scraper;

import ro.autobrand.proba.dto.ScrapedProductDto;

import java.util.List;

public interface Scraper {
    List<ScrapedProductDto> scrape();
}