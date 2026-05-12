package pl.cafteo.jdgflow.tax.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.tax.api.dto.MarkPaidRequest;
import pl.cafteo.jdgflow.tax.api.dto.TaxObligationResponse;
import pl.cafteo.jdgflow.tax.api.dto.TaxPeriodResponse;
import pl.cafteo.jdgflow.tax.api.dto.TaxProfileResponse;
import pl.cafteo.jdgflow.tax.api.dto.UpdateTaxProfileRequest;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;
import pl.cafteo.jdgflow.tax.domain.TaxObligationRepository;
import pl.cafteo.jdgflow.tax.domain.TaxPeriod;
import pl.cafteo.jdgflow.tax.service.TaxPeriodService;
import pl.cafteo.jdgflow.tax.service.TaxProfileService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxPeriodService periodService;
    private final TaxProfileService profileService;
    private final TaxObligationRepository obligationRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/recompute/{year}")
    public List<TaxPeriodResponse> recompute(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable int year) {
        periodService.recomputeYear(user.id(), year);
        return enrichWithObligations(periodService.findYear(user.id(), year));
    }

    @GetMapping("/year/{year}")
    public List<TaxPeriodResponse> year(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable int year) {
        return enrichWithObligations(periodService.findYear(user.id(), year));
    }

    @GetMapping("/months/{year}/{month}")
    public TaxPeriodResponse month(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable int year,
                                    @PathVariable int month) {
        TaxPeriod period = periodService.findMonthOrThrow(user.id(), year, month);
        List<TaxObligation> obligations = obligationRepository.findByPeriodId(period.getId());
        return TaxPeriodResponse.from(period, obligations, objectMapper);
    }

    @GetMapping("/obligations/upcoming")
    public List<TaxObligationResponse> upcoming(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
        return periodService.upcomingObligations(user.id(), until).stream()
                .map(TaxObligationResponse::from)
                .toList();
    }

    @PostMapping("/obligations/{id}/paid")
    public TaxObligationResponse markPaid(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID id,
                                           @RequestBody(required = false) MarkPaidRequest request) {
        MarkPaidRequest body = request == null ? new MarkPaidRequest(null, null) : request;
        return TaxObligationResponse.from(
                periodService.markPaid(user.id(), id, body.paidDate(), body.paidAmount()));
    }

    @DeleteMapping("/obligations/{id}/paid")
    public ResponseEntity<Void> unmarkPaid(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable UUID id) {
        periodService.unmarkPaid(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public TaxProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return TaxProfileResponse.from(profileService.get(user.id()));
    }

    @PutMapping("/profile")
    public TaxProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody UpdateTaxProfileRequest request) {
        return TaxProfileResponse.from(profileService.update(user.id(), request));
    }

    private List<TaxPeriodResponse> enrichWithObligations(List<TaxPeriod> periods) {
        return periods.stream()
                .map(p -> TaxPeriodResponse.from(p, obligationRepository.findByPeriodId(p.getId()), objectMapper))
                .toList();
    }
}
