package pl.cafteo.jdgflow.tax.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.api.dto.UpdateTaxProfileRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    @Transactional
    public User update(UUID userId, UpdateTaxProfileRequest request) {
        User user = get(userId);
        user.updateTaxProfile(
                request.zusMode(),
                request.taxForm(),
                request.vatPayer(),
                request.accountingMethod(),
                request.voluntarySickness(),
                request.ipBoxEligible(),
                request.jointSettlement(),
                request.businessStartDate(),
                request.previousYearRevenue(),
                request.previousYearIncome());
        return user;
    }
}
