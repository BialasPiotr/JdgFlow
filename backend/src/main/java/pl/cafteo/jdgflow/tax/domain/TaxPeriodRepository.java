package pl.cafteo.jdgflow.tax.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxPeriodRepository extends JpaRepository<TaxPeriod, UUID> {
    Optional<TaxPeriod> findByUserIdAndYearAndMonth(UUID userId, int year, int month);

    List<TaxPeriod> findByUserIdAndYearOrderByMonthAsc(UUID userId, int year);
}
