package com.paybridge.payment.persistence;

import com.paybridge.payment.application.TransactionSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<PaymentJpaEntity> from(TransactionSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.orderId() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("orderId")),
                        contains(criteria.orderId())
                ));
            }
            if (criteria.providerPaymentId() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("providerPaymentId")),
                        contains(criteria.providerPaymentId())
                ));
            }
            if (criteria.providerTransactionId() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("providerTransactionId")),
                        contains(criteria.providerTransactionId())
                ));
            }
            if (criteria.provider() != null) {
                predicates.add(criteriaBuilder.equal(root.get("provider"), criteria.provider()));
            }
            if (criteria.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.status()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
