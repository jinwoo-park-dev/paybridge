package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.IdempotencyOperation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, UUID> {

    Optional<IdempotencyKeyJpaEntity> findByOperationAndIdempotencyKey(IdempotencyOperation operation, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select idempotencyKey
            from IdempotencyKeyJpaEntity idempotencyKey
            where idempotencyKey.operation = :operation
              and idempotencyKey.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyKeyJpaEntity> findByOperationAndIdempotencyKeyForUpdate(
        @Param("operation") IdempotencyOperation operation,
        @Param("idempotencyKey") String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select idempotencyKey from IdempotencyKeyJpaEntity idempotencyKey where idempotencyKey.id = :id")
    Optional<IdempotencyKeyJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
