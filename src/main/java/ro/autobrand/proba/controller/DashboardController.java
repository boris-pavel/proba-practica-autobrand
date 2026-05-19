package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ro.autobrand.proba.model.ScrapeRun;
import ro.autobrand.proba.repository.ExchangeRateRepository;
import ro.autobrand.proba.repository.ProductRepository;
import ro.autobrand.proba.repository.ScrapeRunRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepo;
    private final ScrapeRunRepository scrapeRunRepo;
    private final ExchangeRateRepository rateRepo;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("manuallyEditedCount", productRepo.countByManuallyEditedTrue());

        List<ScrapeRun> recent = scrapeRunRepo.findTop5ByOrderByStartedAtDesc();
        model.addAttribute("recentRuns", recent);

        model.addAttribute("lastSuccessAt",
                scrapeRunRepo.findFirstByStatusOrderByStartedAtDesc("SUCCESS")
                        .map(ScrapeRun::getFinishedAt).orElse(null));

        Map<String, Long> currencyStats = new LinkedHashMap<>();
        productRepo.countByCurrency()
                .forEach(c -> currencyStats.put(c.getCurrency(), c.getCount()));
        model.addAttribute("currencyStats", currencyStats);

        rateRepo.findByRateDateAndCurrency(LocalDate.now(), "USD")
                .ifPresent(r -> model.addAttribute("usdRate", r.getRateToRon()));

        return "dashboard";
    }
}