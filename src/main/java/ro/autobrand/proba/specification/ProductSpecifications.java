package ro.autobrand.proba.specification;

import org.springframework.data.jpa.domain.Specification;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;

public class ProductSpecifications {

    private ProductSpecifications() {}   // utility class, no instances

    public static Specification<Product> nameLike(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
        };
    }

    public static Specification<Product> currencyEquals(String currency) {
        return (root, query, cb) -> {
            if (currency == null || currency.isBlank()) return null;
            return cb.equal(root.get("currency"), currency.toUpperCase());
        };
    }

    public static Specification<Product> priceMin(BigDecimal min) {
        return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceMax(BigDecimal max) {
        return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("price"), max);
    }
}