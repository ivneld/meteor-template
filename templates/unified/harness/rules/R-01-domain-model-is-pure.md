---
id: R-01
title: 도메인 모델은 JPA 매핑 외의 프레임워크를 모른다
kind: 구조
applies_to: com.meteor.domain 의 애그리거트, 값 객체(record), *Policy. domain 전체. com.meteor.support.error
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R01_domain_model_is_pure, R01_policies_know_no_persistence, R01_domain_knows_no_outer_layers, R01_domain_uses_only_error_vocabulary_from_support, R01_error_vocabulary_knows_no_framework]
related: [R-04, R-05, R-13]
human_doc: docs/architecture/domain-model.md#R-01
---

# R-01 도메인 모델은 JPA 매핑 외의 프레임워크를 모른다

## 규칙

- 애그리거트, 값 객체, `*Policy` 는 Spring(`org.springframework`), Hibernate 전용 API(`org.hibernate`), client, usecase, api, `*Repository`, `*Reader`, `*Writer` 에 의존하지 않는다.
- 애그리거트와 값 객체가 쓸 수 있는 영속 의존은 `jakarta.persistence` 매핑 어노테이션과 `com.meteor.support.storage.BaseEntity` 뿐이다.
- `*Policy` 는 JPA 와 `support.storage` 도 모른다.
- `com.meteor.domain` 전체는 usecase, api, client 를 모른다. support 에서는 `support.error` 와 `BaseEntity` 만 쓴다.
- `support.error` 는 프레임워크와 다른 패키지를 모른다.

## 왜

JPA 매핑 어노테이션은 메타데이터라 규칙 실행에 영향을 주지 않는다. 그래서 애그리거트가 엔티티여도 `new` 없이 정적 팩토리로 만들어 목 없이 단위 테스트한다. Spring 과 Hibernate 전용 API(`Hibernate.unproxy`, `@Formula` 등)는 분리형으로 옮길 때 걷어내야 할 부채라 처음부터 들이지 않는다. `*Policy` 는 저장과 무관한 순수 규칙이다.

## 이렇게 한다

```java
// Order: JPA 매핑 어노테이션만 쓴다. 규칙과 상태 전이는 순수 자바다
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Column(nullable = false)
    private Long memberId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", getId());
        }
        status = OrderStatus.CANCELLED;
    }
}
```

## 이렇게 하지 않는다

```java
public class Order extends BaseEntity {
    @Formula("(select count(*) from payment p where p.order_id = id)")  // Hibernate 전용 매핑으로 규칙 표현
    private int paymentCount;
    @Autowired OrderRepository repository;                              // 도메인 모델은 빈을 모른다
}
```

## 고치는 법

- 저장소가 필요한 판단이면 사실(건수, 금액)을 `*Writer`/`*Reader` 가 꺼내 넘기고 판단은 애그리거트나 `*Policy` 가 한다(S-04).
- 외부 시스템이 필요하면 usecase 로 올린다.
- 도메인이 쓸 에러는 `ErrorCode` 에 추가하고 `CoreException` 으로 던진다.
