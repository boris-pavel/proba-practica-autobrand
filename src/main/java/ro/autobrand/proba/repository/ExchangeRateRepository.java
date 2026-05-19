package ro.autobrand.proba.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.autobrand.proba.model.ExchangeRate;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByRateDateAndCurrency(LocalDate date, String currency);
    boolean existsByRateDate(LocalDate date);
}