package com.paybridge.payment.persistence;

import com.paybridge.payment.domain.PaymentProvider;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID>, JpaSpecificationExecutor<PaymentJpaEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentJpaEntity payment where payment.id = :id")
    Optional<PaymentJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<PaymentJpaEntity> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);

    Optional<PaymentJpaEntity> findByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);
}
