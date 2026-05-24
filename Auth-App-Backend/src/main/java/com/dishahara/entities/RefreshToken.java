package com.dishahara.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name ="refresh_tokens_jwt_id_idx", columnList = "jwt_id", unique = true),
        @Index(name = "refresh_tokens_user_id_idx", columnList = "user_id")

})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column( name = "jwt_id",unique = true, nullable = false)
    private String jwtId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",  nullable = false, updatable = false)
    private User user;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiredAt;
    private boolean revoked;
    private String replacedByToken;
}
