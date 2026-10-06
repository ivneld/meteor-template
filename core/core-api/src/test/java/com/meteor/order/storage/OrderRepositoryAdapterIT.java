package com.meteor.order.storage;

import com.meteor.ContextTest;
import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderStatus;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryAdapterIT extends ContextTest {

    private final OrderRepository orderRepository;

    OrderRepositoryAdapterIT(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Test
    void valueObjectsSurviveRoundTrip() {
        Address address = new Address("Seoul", "Teheran-ro 1", "06000");
        Order saved = orderRepository.save(Order.place(1L, "keyboard", 2, Money.of(50_000), address));

        Order found = orderRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUnitPrice()).isEqualTo(Money.of(50_000));
        assertThat(found.getShippingAddress()).isEqualTo(address);
        assertThat(found.totalAmount()).isEqualTo(Money.of(100_000));
    }

    @Test
    void saveExistingAppliesDomainState() {
        Order saved = orderRepository
            .save(Order.place(1L, "keyboard", 1, Money.of(1), new Address("Seoul", "Teheran-ro 1", "06000")));
        saved.markPaid();

        orderRepository.save(saved);

        assertThat(orderRepository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

}
