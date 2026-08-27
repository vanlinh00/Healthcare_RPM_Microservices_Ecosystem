package com.healthcare.user.repository;

import com.healthcare.user.model.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {
    List<AuthAuditLog> findTop50ByOrderByTimestampDesc();
    List<AuthAuditLog> findByUserIdOrderByTimestampDesc(String userId);
    Page<AuthAuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
