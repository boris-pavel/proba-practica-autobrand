package ro.autobrand.proba.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ro.autobrand.proba.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    long countByManuallyEditedTrue();

    @Query("SELECT p.currency AS currency, COUNT(p) AS count FROM Product p GROUP BY p.currency")
    List<CurrencyCount> countByCurrency();

    interface CurrencyCount {
        String getCurrency();
        Long getCount();
    }
}