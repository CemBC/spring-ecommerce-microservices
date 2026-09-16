package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM RefreshToken r
            JOIN FETCH r.user
            WHERE r.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
            UPDATE RefreshToken r
            SET r.revoked = true
            WHERE r.user.id = :userId
              AND r.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId);

    long deleteByExpiresAtBefore(LocalDateTime dateTime);
}
