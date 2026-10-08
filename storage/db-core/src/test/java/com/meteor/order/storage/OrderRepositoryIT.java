package com.meteor.order.storage;

import com.meteor.CoreDbContextTest;
import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderStatus;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryIT extends CoreDbContextTest {

    private final OrderRepository orderRepository;

    OrderRepositoryIT(OrderRepository orderRepository) {
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

    @Test
    void countsOrdersByMemberAndStatus() {
        Address address = new Address("Seoul", "Teheran-ro 1", "06000");
        Order paid = orderRepository.save(Order.place(77L, "keyboard", 1, Money.of(1), address));
        paid.markPaid();
        orderRepository.save(paid);
        orderRepository.save(Order.place(77L, "mouse", 1, Money.of(1), address));

        assertThat(orderRepository.countByMemberIdAndStatus(77L, OrderStatus.CREATED)).isEqualTo(1);
        assertThat(orderRepository.countByMemberIdAndStatus(77L, OrderStatus.PAID)).isEqualTo(1);
    }

}
