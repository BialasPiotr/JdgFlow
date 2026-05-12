package pl.cafteo.jdgflow.tax.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {
    Optional<TaxRate> findByYear(int year);
}
