package com.meteor.shipping.api;

import com.meteor.shipping.api.response.ShippingResponse;
import com.meteor.shipping.application.ShippingUseCase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shippings")
public class ShippingController {

    private final ShippingUseCase shippingUseCase;

    public ShippingController(ShippingUseCase shippingUseCase) {
        this.shippingUseCase = shippingUseCase;
    }

    @GetMapping
    public ShippingResponse getByOrder(@RequestParam Long orderId) {
        return ShippingResponse.from(shippingUseCase.findByOrder(orderId));
    }

    @GetMapping("/{shippingId}")
    public ShippingResponse get(@PathVariable Long shippingId) {
        return ShippingResponse.from(shippingUseCase.find(shippingId));
    }

    @PostMapping("/{shippingId}/ship")
    public ShippingResponse ship(@PathVariable Long shippingId) {
        return ShippingResponse.from(shippingUseCase.ship(shippingId));
    }

    @PostMapping("/{shippingId}/deliver")
    public ShippingResponse deliver(@PathVariable Long shippingId) {
        return ShippingResponse.from(shippingUseCase.deliver(shippingId));
    }

}
