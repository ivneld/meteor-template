package com.meteor.query.application;

import com.meteor.ContextTest;
import com.meteor.member.application.MemberRegisterCommand;
import com.meteor.member.application.MemberUseCase;
import com.meteor.member.domain.Email;
import com.meteor.order.application.OrderPlaceCommand;
import com.meteor.order.application.OrderUseCase;
import com.meteor.payment.application.PaymentPayCommand;
import com.meteor.payment.application.PaymentUseCase;
import com.meteor.payment.domain.PaymentMethod;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import com.meteor.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDetailQueryIT extends ContextTest {

    private final MemberUseCase memberUseCase;

    private final OrderUseCase orderUseCase;

    private final PaymentUseCase paymentUseCase;

    private final OrderDetailQuery orderDetailQuery;

    OrderDetailQueryIT(MemberUseCase memberUseCase, OrderUseCase orderUseCase, PaymentUseCase paymentUseCase,
            OrderDetailQuery orderDetailQuery) {
        this.memberUseCase = memberUseCase;
        this.orderUseCase = orderUseCase;
        this.paymentUseCase = paymentUseCase;
        this.orderDetailQuery = orderDetailQuery;
    }

    @Test
    void joinsFourContextsReadOnly() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("query@example.com"), "kim"));
        Long orderId = orderUseCase
            .place(new OrderPlaceCommand(memberId, "keyboard", 2, Money.of(50_000),
                    new Address("Seoul", "Teheran-ro 1", "06000")))
            .order()
            .getId();

        OrderDetailView before = orderDetailQuery.findById(orderId);
        assertThat(before.orderStatus()).isEqualTo("CREATED");
        assertThat(before.paidAmount()).isNull();
        assertThat(before.shippingStatus()).isNull();

        paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD));

        OrderDetailView after = orderDetailQuery.findById(orderId);
        assertThat(after.memberName()).isEqualTo("kim");
        assertThat(after.totalAmount()).isEqualTo(100_000);
        assertThat(after.orderStatus()).isEqualTo("PAID");
        assertThat(after.paidAmount()).isEqualTo(100_000);
        assertThat(after.paymentMethod()).isEqualTo("CARD");
        assertThat(after.shippingStatus()).isEqualTo("READY");
    }

    @Test
    void unknownOrderIsNotFound() {
        assertThatThrownBy(() -> orderDetailQuery.findById(999_999L)).isInstanceOf(CoreException.class);
    }

}
