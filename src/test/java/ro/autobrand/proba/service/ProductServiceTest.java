package ro.autobrand.proba.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    ProductRepository repo;
    ProductService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductRepository.class);
        service = new ProductService(repo);
    }

    @Test
    void inserts_new_product() {
        when(repo.findByName("Apple")).thenReturn(Optional.empty());

        service.upsertAll(List.of(scraped("Apple", "9.99")));

        verify(repo).save(argThat(p ->
                p.getName().equals("Apple") &&
                        p.getPrice().compareTo(new BigDecimal("9.99")) == 0
        ));
    }

    @Test
    void updates_existing_when_not_manually_edited() {
        Product existing = Product.builder()
                .id(1L)
                .name("Apple")
                .price(new BigDecimal("5.00"))
                .currency("USD")
                .manuallyEdited(false)
                .firstSeen(LocalDateTime.now().minusDays(1))
                .build();
        when(repo.findByName("Apple")).thenReturn(Optional.of(existing));

        service.upsertAll(List.of(scraped("Apple", "9.99")));

        verify(repo).save(argThat(p ->
                p.getPrice().compareTo(new BigDecimal("9.99")) == 0 &&
                        !p.isManuallyEdited()
        ));
    }

    @Test
    void preserves_manually_edited_fields_only_updates_last_scraped() {
        LocalDateTime oldScraped = LocalDateTime.now().minusHours(2);
        Product existing = Product.builder()
                .id(2L)
                .name("Banana")
                .price(new BigDecimal("3.00"))
                .currency("EUR")
                .description("Edited manually")
                .manuallyEdited(true)
                .firstSeen(LocalDateTime.now().minusDays(2))
                .lastScraped(oldScraped)
                .build();
        when(repo.findByName("Banana")).thenReturn(Optional.of(existing));

        service.upsertAll(List.of(scraped("Banana", "4.50")));

        verify(repo).save(argThat(p ->
                p.getPrice().compareTo(new BigDecimal("3.00")) == 0 &&     // NESCHIMBAT
                        p.getDescription().equals("Edited manually") &&             // NESCHIMBAT
                        p.isManuallyEdited() &&
                        p.getLastScraped().isAfter(oldScraped)                       // UPDATED
        ));
    }

    private ScrapedProductDto scraped(String name, String price) {
        return ScrapedProductDto.builder()
                .name(name)
                .price(new BigDecimal(price))
                .currency("USD")
                .description("desc")
                .imageUrl("https://img")
                .sourceUrl("https://src")
                .build();
    }
}