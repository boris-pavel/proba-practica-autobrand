package ro.autobrand.proba.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    ProductRepository repository;

    @Test
    void saves_and_finds_by_name() {
        Product saved = repository.save(Product.builder()
                .name("Apple")
                .price(new BigDecimal("9.99"))
                .currency("USD")
                .build());

        Optional<Product> found = repository.findByName("Apple");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getPrice()).isEqualByComparingTo("9.99");
    }

    @Test
    void enforces_unique_name() {
        repository.save(Product.builder()
                .name("Banana")
                .price(BigDecimal.ONE)
                .currency("USD")
                .build());

        assertThatThrownBy(() ->
                repository.saveAndFlush(Product.builder()
                        .name("Banana")
                        .price(BigDecimal.TEN)
                        .currency("USD")
                        .build())
        ).hasCauseInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
}