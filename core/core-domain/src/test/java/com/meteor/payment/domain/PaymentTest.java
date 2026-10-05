package com.meteor.payment.domain;

import com.meteor.shared.Money;
import com.meteor.shared.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void approvesPositiveAmount() {
        Payment payment = Payment.approve(1L, Money.of(1000), PaymentMethod.CARD);

        assertThat(payment.isNew()).isTrue();
        assertThat(payment.getAmount()).isEqualTo(Money.of(1000));
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Payment.approve(1L, Money.ZERO, PaymentMethod.CARD))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
