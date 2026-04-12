package com.paybridge.payment.persistence;

import com.paybridge.payment.application.TransactionExportCriteria;
import com.paybridge.payment.application.TransactionSearchCriteria;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<PaymentJpaEntity> from(TransactionSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addContainsPredicate(predicates, criteriaBuilder, root.get("orderId"), criteria.orderId());
            addContainsPredicate(predicates, criteriaBuilder, root.get("providerPaymentId"), criteria.providerPaymentId());
            addContainsPredicate(predicates, criteriaBuilder, root.get("providerTransactionId"), criteria.providerTransactionId());
            addProviderAndStatusPredicates(predicates, criteriaBuilder, root, criteria.provider(), criteria.status());

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<PaymentJpaEntity> from(TransactionExportCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addProviderAndStatusPredicates(predicates, criteriaBuilder, root, criteria.provider(), criteria.status());
            addApprovedFromPredicate(predicates, criteriaBuilder, root.get("approvedAt"), criteria.approvedFrom());
            addApprovedToPredicate(predicates, criteriaBuilder, root.get("approvedAt"), criteria.approvedTo());

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addContainsPredicate(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Path<String> path,
            String value
    ) {
        if (value != null) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(path), contains(value)));
        }
    }

    private static void addProviderAndStatusPredicates(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            jakarta.persistence.criteria.Root<PaymentJpaEntity> root,
            Object provider,
            Object status
    ) {
        if (provider != null) {
            predicates.add(criteriaBuilder.equal(root.get("provider"), provider));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), status));
        }
    }

    private static void addApprovedFromPredicate(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Path<OffsetDateTime> approvedAtPath,
            Instant approvedFrom
    ) {
        if (approvedFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(approvedAtPath, toOffsetDateTime(approvedFrom)));
        }
    }

    private static void addApprovedToPredicate(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Path<OffsetDateTime> approvedAtPath,
            Instant approvedTo
    ) {
        if (approvedTo != null) {
            predicates.add(criteriaBuilder.lessThan(approvedAtPath, toOffsetDateTime(approvedTo)));
        }
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
