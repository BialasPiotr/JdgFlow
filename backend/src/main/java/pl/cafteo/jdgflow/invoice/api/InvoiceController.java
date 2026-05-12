package pl.cafteo.jdgflow.invoice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.common.csv.CsvResponse;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.invoice.api.dto.InvoiceResponse;
import pl.cafteo.jdgflow.invoice.api.dto.PageResponse;
import pl.cafteo.jdgflow.invoice.domain.Invoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;
import pl.cafteo.jdgflow.invoice.service.InvoiceCsvExporter;
import pl.cafteo.jdgflow.invoice.service.InvoiceQueryService;
import pl.cafteo.jdgflow.invoice.service.InvoiceSyncService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceQueryService queryService;
    private final InvoiceSyncService syncService;
    private final InvoiceCsvExporter csvExporter;

    @GetMapping
    public PageResponse<InvoiceResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "issueDate"));
        Page<Invoice> result = queryService.list(user.id(), status, from, to, pageable);
        return PageResponse.from(result, InvoiceResponse::from);
    }

    @GetMapping(path = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        var invoices = queryService.listAll(user.id(), status, from, to);
        String filename = "faktury-" + LocalDate.now() + ".csv";
        return CsvResponse.attachment(filename, csvExporter.toCsv(invoices));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID id) {
        return InvoiceResponse.from(queryService.byId(user.id(), id));
    }

    @PostMapping("/sync")
    public ResponseEntity<InvoiceSyncService.SyncResult> sync(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(syncService.syncAll(user.id()));
    }
}
