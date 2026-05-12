package pl.cafteo.jdgflow.notification.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.notification.api.dto.NotificationResponse;
import pl.cafteo.jdgflow.notification.domain.NotificationRepository;
import pl.cafteo.jdgflow.notification.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(user.id()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationResponse> sendTest(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificationResponse.from(notificationService.sendTest(user.id())));
    }
}
