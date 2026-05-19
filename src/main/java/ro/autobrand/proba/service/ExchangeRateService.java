package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import ro.autobrand.proba.model.ExchangeRate;
import ro.autobrand.proba.repository.ExchangeRateRepository;
import ro.autobrand.proba.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private static final Set<String> SUPPORTED =
            Set.of("USD", "EUR", "GBP", "CHF", "JPY");

    /** Pattern pentru elementele <Rate currency="X" multiplier="Y">Z</Rate> din BNR XML. */
    private static final Pattern RATE_PATTERN = Pattern.compile(
            "<Rate currency=\"([A-Z]{3})\"(?: multiplier=\"(\\d+)\")?[^>]*>([0-9.]+)</Rate>"
    );

    private final ExchangeRateRepository rateRepo;
    private final ProductRepository productRepo;
    private final RestClient restClient = RestClient.create();

    @Transactional
    public void ensureTodayRates() {
        LocalDate today = LocalDate.now();
        if (rateRepo.existsByRateDate(today)) {
            log.debug("Rates for {} already fetched", today);
            return;
        }
        try {
            String xml = restClient.get()
                    .uri("https://www.bnr.ro/nbrfxrates.xml")
                    .retrieve()
                    .body(String.class);
            parseAndStore(xml, today);
        } catch (Exception e) {
            log.error("BNR fetch failed", e);
        }
    }

    void parseAndStore(String xml, LocalDate date) {
        if (xml == null) return;
        Matcher m = RATE_PATTERN.matcher(xml);
        int saved = 0;
        while (m.find()) {
            String currency = m.group(1);
            if (!SUPPORTED.contains(currency)) continue;

            int multiplier = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            BigDecimal rate = new BigDecimal(m.group(3));

            rateRepo.save(ExchangeRate.builder()
                    .rateDate(date)
                    .currency(currency)
                    .rateToRon(rate)
                    .multiplier(multiplier)
                    .build());
            saved++;
        }
        log.info("Saved {} BNR rates for {}", saved, date);
    }

    @Transactional
    public void recomputeRon() {
        LocalDate today = LocalDate.now();
        int updated = 0;
        for (var p : productRepo.findAll()) {
            if ("RON".equalsIgnoreCase(p.getCurrency())) {
                p.setPriceRon(p.getPrice());
                productRepo.save(p);
                updated++;
                continue;
            }
            var rateOpt = rateRepo.findByRateDateAndCurrency(today, p.getCurrency());
            if (rateOpt.isPresent()) {
                var r = rateOpt.get();
                BigDecimal ron = p.getPrice()
                        .multiply(r.getRateToRon())
                        .divide(BigDecimal.valueOf(r.getMultiplier()), 2, RoundingMode.HALF_UP);
                p.setPriceRon(ron);
                productRepo.save(p);
                updated++;
            }
        }
        log.info("Recomputed RON price for {} products", updated);
    }
}