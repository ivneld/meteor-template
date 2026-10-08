package com.meteor.order.storage;

import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 주문 영속 모델. 도메인을 모르므로 값 객체는 컬럼으로 펼치고 상태는 문자열로 저장한다. 회원 테이블과 FK 를 걸지 않는다(S-04).
 */
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 20)
    private String status;

    protected OrderEntity() {
    }

    public OrderEntity(Long memberId, String productName, int quantity, long unitPrice, String city, String street,
            String zipCode, String status) {
        this.memberId = memberId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getStatus() {
        return status;
    }

}
