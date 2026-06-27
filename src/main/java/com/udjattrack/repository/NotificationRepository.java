package com.udjattrack.repository;

import com.udjattrack.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    java.util.List<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findAllByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByRecipientUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Transactional
    void deleteAllByRecipientUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);
}
