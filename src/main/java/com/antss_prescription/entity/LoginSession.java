package com.antss_prescription.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_sessions")
@Data
public class LoginSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 2000)
    private String token;

    @Column(unique = true, length = 2000)
    private String refreshToken;

    private String deviceInfo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean expired;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expired = false;
    }
}
