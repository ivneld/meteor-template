package com.meteor.query.api;

import com.meteor.query.application.OrderDetailQuery;
import com.meteor.query.application.OrderDetailView;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 읽기 모델(View)은 원시 타입과 문자열뿐이라 응답 DTO 로 다시 감싸지 않고 그대로 내보낸다.
 */
@RestController
@RequestMapping("/api/v1/query/orders")
public class OrderDetailController {

    private final OrderDetailQuery orderDetailQuery;

    public OrderDetailController(OrderDetailQuery orderDetailQuery) {
        this.orderDetailQuery = orderDetailQuery;
    }

    @GetMapping("/{orderId}")
    public OrderDetailView get(@PathVariable Long orderId) {
        return orderDetailQuery.findById(orderId);
    }

}
