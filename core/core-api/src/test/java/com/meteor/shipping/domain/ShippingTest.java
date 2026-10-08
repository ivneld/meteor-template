package com.meteor.shipping.domain;

import com.meteor.shared.Address;
import com.meteor.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingTest {

    private static final Address ADDRESS = new Address("Seoul", "Teheran-ro 1", "06000");

    @Test
    void followsStateMachine() {
        Shipping shipping = Shipping.prepare(1L, ADDRESS);
        assertThat(shipping.getStatus()).isEqualTo(ShippingStatus.READY);

        shipping.ship();
        assertThat(shipping.getStatus()).isEqualTo(ShippingStatus.SHIPPED);

        shipping.deliver();
        assertThat(shipping.getStatus()).isEqualTo(ShippingStatus.DELIVERED);
    }

    @Test
    void rejectsSkippingStates() {
        Shipping shipping = Shipping.prepare(1L, ADDRESS);

        assertThatThrownBy(shipping::deliver).isInstanceOf(CoreException.class);
    }

}
