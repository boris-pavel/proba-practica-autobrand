package ro.autobrand.proba.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_run")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(nullable = false, length = 20)
    private String status;          // RUNNING, SUCCESS, FAILED

    @Column(name = "products_total")
    private Integer productsTotal;

    @Column(name = "products_new")
    private Integer productsNew;

    @Column(name = "products_updated")
    private Integer productsUpdated;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}