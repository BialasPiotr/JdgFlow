package pl.cafteo.jdgflow.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

    @Query("""
            select count(n) > 0 from Notification n
            where n.obligationId = :obligationId
              and n.daysBefore = :daysBefore
              and n.delivered = true
            """)
    boolean existsDeliveredReminder(@Param("obligationId") UUID obligationId,
                                     @Param("daysBefore") Integer daysBefore);
}
