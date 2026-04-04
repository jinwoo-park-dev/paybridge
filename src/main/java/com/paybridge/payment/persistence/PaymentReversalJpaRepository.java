package com.paybridge.payment.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReversalJpaRepository extends JpaRepository<PaymentReversalJpaEntity, UUID> {

    List<PaymentReversalJpaEntity> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    Optional<PaymentReversalJpaEntity> findByPaymentIdAndProviderReversalId(UUID paymentId, String providerReversalId);
}
