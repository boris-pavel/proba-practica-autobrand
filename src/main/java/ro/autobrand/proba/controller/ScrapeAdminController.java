package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.service.ProductService;
import ro.autobrand.proba.service.ScrapingService;

@Controller
@RequestMapping("/admin/scrape")
@RequiredArgsConstructor
public class ScrapeAdminController {

    private final ScrapingService scrapingService;

    @PostMapping
    public String runNow(RedirectAttributes ra) {
        ProductService.UpsertResult result = scrapingService.runScrape();
        ra.addFlashAttribute("success",
                "Scrape: %d adăugate, %d actualizate, %d păstrate (editate manual)"
                        .formatted(result.inserted(), result.updated(), result.preserved()));
        return "redirect:/products";
    }
}