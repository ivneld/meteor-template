package com.meteor.shipping.storage;

import com.meteor.CoreDbContextTest;
import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingRepositoryIT extends CoreDbContextTest {

    private final ShippingRepository shippingRepository;

    ShippingRepositoryIT(ShippingRepository shippingRepository) {
        this.shippingRepository = shippingRepository;
    }

    @Test
    void findsByOrderId() {
        Shipping saved = shippingRepository.save(Shipping.prepare(42L, new Address("Seoul", "Teheran-ro 1", "06000")));

        assertThat(shippingRepository.findByOrderId(42L)).map(Shipping::getId).contains(saved.getId());
        assertThat(shippingRepository.findByOrderId(43L)).isEmpty();
    }

}
