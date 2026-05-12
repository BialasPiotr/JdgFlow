package pl.cafteo.jdgflow.tax.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxObligationRepository extends JpaRepository<TaxObligation, UUID> {
    List<TaxObligation> findByPeriodId(UUID periodId);

    List<TaxObligation> findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(
            UUID userId, LocalDate from, LocalDate to);

    List<TaxObligation> findByUserIdAndPaidDateIsNullOrderByDeadlineAsc(UUID userId);
}
