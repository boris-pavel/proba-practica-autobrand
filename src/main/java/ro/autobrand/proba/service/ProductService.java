package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;
import org.springframework.data.jpa.domain.Specification;
import ro.autobrand.proba.specification.ProductSpecifications;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository repository;

    @Transactional
    public UpsertResult upsertAll(List<ScrapedProductDto> scraped) {
        int inserted = 0;
        int updated = 0;
        int preserved = 0;
        LocalDateTime now = LocalDateTime.now();

        for (ScrapedProductDto s : scraped) {
            Optional<Product> existing = repository.findByName(s.getName());
            if (existing.isEmpty()) {
                repository.save(Product.builder()
                        .name(s.getName())
                        .description(s.getDescription())
                        .price(s.getPrice())
                        .currency(s.getCurrency())
                        .imageUrl(s.getImageUrl())
                        .sourceUrl(s.getSourceUrl())
                        .manuallyEdited(false)
                        .firstSeen(now)
                        .lastScraped(now)
                        .updatedAt(now)
                        .build());
                inserted++;
            } else {
                Product p = existing.get();
                if (p.isManuallyEdited()) {
                    p.setLastScraped(now);
                    repository.save(p);
                    preserved++;
                } else {
                    p.setDescription(s.getDescription());
                    p.setPrice(s.getPrice());
                    p.setCurrency(s.getCurrency());
                    p.setImageUrl(s.getImageUrl());
                    p.setSourceUrl(s.getSourceUrl());
                    p.setLastScraped(now);
                    repository.save(p);
                    updated++;
                }
            }
        }
        log.info("Upsert done: {} inserted, {} updated, {} preserved (manually edited)",
                inserted, updated, preserved);
        return new UpsertResult(inserted, updated, preserved);
    }

    public record UpsertResult(int inserted, int updated, int preserved) {
        public int total() { return inserted + updated + preserved; }
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Product> search(
            String name, String currency, BigDecimal minPrice, BigDecimal maxPrice,
            org.springframework.data.domain.Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameLike(name),
                ProductSpecifications.currencyEquals(currency),
                ProductSpecifications.priceMin(minPrice),
                ProductSpecifications.priceMax(maxPrice)
        );
        return repository.findAll(spec, pageable);
    }
}