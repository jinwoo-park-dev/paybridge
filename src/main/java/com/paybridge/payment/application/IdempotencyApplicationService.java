package com.paybridge.payment.application;

import com.paybridge.payment.domain.IdempotencyOperation;
import com.paybridge.payment.persistence.IdempotencyKeyJpaEntity;
import com.paybridge.payment.persistence.IdempotencyKeyJpaRepository;
import com.paybridge.payment.persistence.IdempotencyRecordStatus;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyApplicationService {

    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(5);

    private final IdempotencyKeyJpaRepository idempotencyKeyRepository;

    public IdempotencyApplicationService(IdempotencyKeyJpaRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyReservationResult reserve(IdempotencyOperation operation, String idempotencyKey, String requestHash) {
        Objects.requireNonNull(operation, "operation must not be null");
        String normalizedKey = normalizeRequired(idempotencyKey, "idempotencyKey");
        String normalizedRequestHash = normalizeRequired(requestHash, "requestHash");
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime lockedUntil = now.plus(DEFAULT_LOCK_DURATION);

        return idempotencyKeyRepository.findByOperationAndIdempotencyKeyForUpdate(operation, normalizedKey)
                .map(existing -> evaluateExisting(existing, normalizedRequestHash, lockedUntil))
                .orElseGet(() -> createReservation(operation, normalizedKey, normalizedRequestHash, lockedUntil));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID idempotencyRecordId, UUID resultPaymentId) {
        IdempotencyKeyJpaEntity entity = idempotencyKeyRepository.findByIdForUpdate(idempotencyRecordId)
                .orElseThrow(() -> new PayBridgeException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Idempotency record not found: " + idempotencyRecordId
                ));

        entity.setStatus(IdempotencyRecordStatus.COMPLETED);
        entity.setResultPaymentId(resultPaymentId);
        entity.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC));
        idempotencyKeyRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID idempotencyRecordId) {
        IdempotencyKeyJpaEntity entity = idempotencyKeyRepository.findByIdForUpdate(idempotencyRecordId)
                .orElseThrow(() -> new PayBridgeException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Idempotency record not found: " + idempotencyRecordId
                ));

        entity.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC));
        idempotencyKeyRepository.save(entity);
    }

    private IdempotencyReservationResult evaluateExisting(
            IdempotencyKeyJpaEntity existing,
            String requestHash,
            OffsetDateTime newLockedUntil
    ) {
        if (!existing.getRequestHash().equals(requestHash)) {
            return new IdempotencyReservationResult(
                    IdempotencyDecision.CONFLICT,
                    existing.getId(),
                    existing.getResultPaymentId(),
                    existing.getLockedUntil()
            );
        }

        if (existing.getStatus() == IdempotencyRecordStatus.COMPLETED) {
            return new IdempotencyReservationResult(
                    IdempotencyDecision.REPLAY,
                    existing.getId(),
                    existing.getResultPaymentId(),
                    existing.getLockedUntil()
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (existing.getLockedUntil() != null && existing.getLockedUntil().isAfter(now)) {
            return new IdempotencyReservationResult(
                    IdempotencyDecision.IN_PROGRESS,
                    existing.getId(),
                    existing.getResultPaymentId(),
                    existing.getLockedUntil()
            );
        }

        existing.setLockedUntil(newLockedUntil);
        existing.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        IdempotencyKeyJpaEntity saved = idempotencyKeyRepository.save(existing);
        return new IdempotencyReservationResult(
                IdempotencyDecision.RESERVED,
                saved.getId(),
                saved.getResultPaymentId(),
                saved.getLockedUntil()
        );
    }

    private IdempotencyReservationResult createReservation(
            IdempotencyOperation operation,
            String idempotencyKey,
            String requestHash,
            OffsetDateTime lockedUntil
    ) {
        IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setOperation(operation);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setRequestHash(requestHash);
        entity.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        entity.setLockedUntil(lockedUntil);
        try {
            IdempotencyKeyJpaEntity saved = idempotencyKeyRepository.saveAndFlush(entity);
            return new IdempotencyReservationResult(
                    IdempotencyDecision.RESERVED,
                    saved.getId(),
                    saved.getResultPaymentId(),
                    saved.getLockedUntil()
            );
        } catch (DataIntegrityViolationException ex) {
            IdempotencyKeyJpaEntity existing = idempotencyKeyRepository.findByOperationAndIdempotencyKeyForUpdate(operation, idempotencyKey)
                    .orElseThrow(() -> ex);
            return evaluateExisting(existing, requestHash, lockedUntil);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }
}
