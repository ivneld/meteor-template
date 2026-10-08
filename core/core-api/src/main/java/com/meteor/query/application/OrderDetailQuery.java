package com.meteor.query.application;

import java.util.Optional;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * query 컨텍스트. 여러 컨텍스트의 테이블을 읽기 전용으로 JOIN 해 화면용 모델을 만든다(S-06 의 유일한 읽기 JOIN 허용 지점).
 *
 * <p>
 * 다른 컨텍스트의 Repository 나 애그리거트를 쓰지 않고 SQL 로만 읽는다. 쓰기는 하지 않는다. 컨텍스트 소유 테이블의 스키마가 바뀌면 이 SQL
 * 도 함께 바꿔야 하므로, 그 결합은 이 패키지 안에만 가둔다.
 */
@Service
public class OrderDetailQuery {

    private static final String SQL = """
            select o.id as order_id, o.product_name, o.quantity, o.unit_price, o.status as order_status,
                   m.name as member_name, m.email as member_email,
                   p.amount as paid_amount, p.method as payment_method,
                   s.status as shipping_status
            from orders o
            join member m on m.id = o.member_id
            left join payment p on p.order_id = o.id
            left join shipping s on s.order_id = o.id
            where o.id = :orderId
            """;

    private final JdbcClient jdbcClient;

    public OrderDetailQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public OrderDetailView findById(Long orderId) {
        Optional<OrderDetailView> view = jdbcClient.sql(SQL)
            .param("orderId", orderId)
            .query((rs, rowNum) -> new OrderDetailView(rs.getLong("order_id"), rs.getString("product_name"),
                    rs.getInt("quantity"), rs.getLong("unit_price"), rs.getLong("unit_price") * rs.getInt("quantity"),
                    rs.getString("order_status"), rs.getString("member_name"), rs.getString("member_email"),
                    rs.getObject("paid_amount", Long.class), rs.getString("payment_method"),
                    rs.getString("shipping_status")))
            .optional();
        return view.orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
    }

}
