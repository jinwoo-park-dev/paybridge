package com.paybridge.providers.stripe.webhook;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StripeWebhookQueryService {

    private final StripeWebhookEventJpaRepository stripeWebhookEventJpaRepository;

    public StripeWebhookQueryService(StripeWebhookEventJpaRepository stripeWebhookEventJpaRepository) {
        this.stripeWebhookEventJpaRepository = stripeWebhookEventJpaRepository;
    }

    public List<StripeWebhookEventView> findRecent() {
        return stripeWebhookEventJpaRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(entity -> new StripeWebhookEventView(
                        entity.getId(),
                        entity.getProvider(),
                        entity.getProviderEventId(),
                        entity.getEventType(),
                        entity.isSignatureVerified(),
                        entity.getProcessingStatus(),
                        entity.getCorrelationId(),
                        entity.getLastError(),
                        entity.getProcessedAt(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
