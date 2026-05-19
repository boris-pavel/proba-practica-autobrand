package ro.autobrand.proba.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.autobrand.proba.model.ScrapeRun;

import java.util.List;
import java.util.Optional;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
    List<ScrapeRun> findTop5ByOrderByStartedAtDesc();
    Optional<ScrapeRun> findFirstByStatusOrderByStartedAtDesc(String status);
}