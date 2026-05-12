package pl.cafteo.jdgflow.dashboard.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowResponse;
import pl.cafteo.jdgflow.dashboard.service.CashflowPdfExporter;
import pl.cafteo.jdgflow.dashboard.service.CashflowService;

import java.time.Year;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CashflowService cashflowService;
    private final CashflowPdfExporter pdfExporter;
    private final UserRepository userRepository;

    @GetMapping("/cashflow")
    public CashflowResponse cashflow(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();
        return cashflowService.computeYear(user.id(), targetYear);
    }

    @GetMapping(path = "/cashflow.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> cashflowPdf(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        CashflowResponse data = cashflowService.computeYear(user.getId(), targetYear);
        byte[] pdf = pdfExporter.toPdf(user, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cashflow-" + targetYear + ".pdf\"")
                .body(pdf);
    }
}
