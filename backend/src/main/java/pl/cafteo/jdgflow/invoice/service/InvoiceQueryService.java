package pl.cafteo.jdgflow.invoice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.invoice.domain.Invoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceRepository;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final InvoiceRepository repository;

    public Page<Invoice> list(UUID userId, InvoiceStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        return repository.findAll(buildSpec(userId, status, from, to), pageable);
    }

    public List<Invoice> listAll(UUID userId, InvoiceStatus status, LocalDate from, LocalDate to) {
        return repository.findAll(buildSpec(userId, status, from, to),
                Sort.by(Sort.Direction.DESC, "issueDate"));
    }

    private static Specification<Invoice> buildSpec(UUID userId, InvoiceStatus status, LocalDate from, LocalDate to) {
        Specification<Invoice> spec = (root, query, cb) -> cb.equal(root.get("userId"), userId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issueDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issueDate"), to));
        }
        return spec;
    }

    public Invoice byId(UUID userId, UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound("Invoice not found"));
        if (!invoice.getUserId().equals(userId)) {
            throw BusinessException.notFound("Invoice not found");
        }
        return invoice;
    }
}
