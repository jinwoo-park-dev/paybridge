package com.paybridge.support.metrics;

import com.paybridge.payment.domain.PaymentProvider;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public PaymentMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPaymentApproved(PaymentProvider provider) {
        meterRegistry.counter("paybridge.payment.approved", "provider", normalize(provider.name())).increment();
    }

    public void recordReversal(PaymentProvider provider, String reversalType) {
        meterRegistry.counter("paybridge.payment.reversal", "provider", normalize(provider.name()), "reversalType", normalize(reversalType)).increment();
    }

    public void recordWebhookReceived(PaymentProvider provider, String eventType) {
        meterRegistry.counter("paybridge.webhook.received", "provider", normalize(provider.name()), "eventType", normalize(eventType)).increment();
    }

    public void recordWebhookDuplicate(PaymentProvider provider, String eventType) {
        meterRegistry.counter("paybridge.webhook.duplicate", "provider", normalize(provider.name()), "eventType", normalize(eventType)).increment();
    }

    public void recordWebhookRejected(PaymentProvider provider) {
        meterRegistry.counter("paybridge.webhook.rejected", "provider", normalize(provider.name())).increment();
    }

    public void recordOutboxAppended(String eventType) {
        meterRegistry.counter("paybridge.outbox.appended", "eventType", normalize(eventType)).increment();
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT).replace('.', '_');
    }
}
