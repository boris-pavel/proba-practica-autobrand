package ro.autobrand.proba.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rate_date", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "rate_to_ron", nullable = false, precision = 12, scale = 6)
    private BigDecimal rateToRon;

    @Column(nullable = false)
    @Builder.Default
    private int multiplier = 1;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @PrePersist
    void onCreate() {
        if (fetchedAt == null) fetchedAt = LocalDateTime.now();
    }
}