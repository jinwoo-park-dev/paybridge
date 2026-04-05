package com.paybridge.providers.stripe.webhook;

import com.paybridge.payment.domain.PaymentProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventJpaRepository extends JpaRepository<StripeWebhookEventJpaEntity, UUID> {

    Optional<StripeWebhookEventJpaEntity> findByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);

    List<StripeWebhookEventJpaEntity> findTop50ByOrderByCreatedAtDesc();
}
