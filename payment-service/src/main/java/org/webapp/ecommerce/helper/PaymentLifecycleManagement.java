package org.webapp.ecommerce.helper;

import org.webapp.ecommerce.entity.PaymentStatus;
import org.webapp.ecommerce.exception.PaymentStatusTransitionException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class PaymentLifecycleManagement {

    private static final Map<PaymentStatus, Set<PaymentStatus>> PAYMENT_STATUS_MAP = Map.of(

            PaymentStatus.INITIATED,
            Set.of(PaymentStatus.PENDING, PaymentStatus.CANCELED),

            PaymentStatus.PENDING,
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELED),

            PaymentStatus.SUCCEEDED,
            Set.of(PaymentStatus.REFUND_INITIATED),

            PaymentStatus.REFUND_INITIATED,
            Set.of(PaymentStatus.REFUND_COMPLETED),

            PaymentStatus.FAILED, Collections.emptySet(),

            PaymentStatus.CANCELED, Collections.emptySet(),

            PaymentStatus.REFUND_COMPLETED, Collections.emptySet()
    );

    public static void validateStateTransition(PaymentStatus oldStatus, PaymentStatus currentStatus){
        if (oldStatus == currentStatus) {
            return;
        }

        Set<PaymentStatus> allowedValues = PAYMENT_STATUS_MAP.getOrDefault(oldStatus, Collections.emptySet());

        if(!allowedValues.contains(currentStatus)){
            throw new PaymentStatusTransitionException(String.format("Invalid status transition from %s to %s", oldStatus, currentStatus));
        }
    }

}
