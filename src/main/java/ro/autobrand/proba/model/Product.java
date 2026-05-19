package ro.autobrand.proba.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "price_ron", precision = 12, scale = 2)
    private BigDecimal priceRon;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "manually_edited", nullable = false)
    @Builder.Default
    private boolean manuallyEdited = false;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_scraped", nullable = false)
    private LocalDateTime lastScraped;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeen == null) firstSeen = now;
        if (lastScraped == null) lastScraped = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}