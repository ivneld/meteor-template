package com.meteor.order.api;

import com.meteor.order.api.request.OrderPlaceRequest;
import com.meteor.order.api.response.OrderResponse;
import com.meteor.order.application.OrderUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    public OrderResponse place(@RequestBody @Valid OrderPlaceRequest request) {
        return OrderResponse.from(orderUseCase.place(request.toCommand()));
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId) {
        return OrderResponse.from(orderUseCase.find(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return OrderResponse.from(orderUseCase.cancel(orderId));
    }

}
