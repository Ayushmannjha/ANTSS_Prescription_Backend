package com.antss_prescription.repository;

import com.antss_prescription.entity.LoginSession;
import com.antss_prescription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    Optional<LoginSession> findByTokenAndExpiredFalse(String token);
    Optional<LoginSession> findByRefreshTokenAndExpiredFalse(String refreshToken);

    @Modifying
    @Query("UPDATE LoginSession s SET s.expired = true WHERE s.user = :user")
    void expireAllSessionsForUser(User user);
}
