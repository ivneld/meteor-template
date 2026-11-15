---
id: R-10
title: api 는 usecase 와 도메인 서비스만 부르고 애그리거트는 읽기만 한다
kind: 구조
applies_to: com.meteor.api
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R10_api_depends_on_use_cases_and_domain_services_only, R10_api_only_reads_aggregates]
related: [R-11, R-12, R-19]
human_doc: docs/architecture/api.md#R-10
---

# R-10 api 는 usecase 와 도메인 서비스만 부르고 애그리거트는 읽기만 한다

## 규칙

- api 는 `*Repository`, `*Policy`, 저장소 인프라(`support.storage`), client 를 쓰지 않는다. `*UseCase` 나 `*Reader`·`*Writer` 를 부른다.
- 돌려받은 애그리거트(엔티티)에서는 접근자(`get*`, `is*`, `has*`, `*Amount`, `equals`/`hashCode`/`toString`)만 부른다.

## 왜

컨트롤러가 애그리거트의 상태 변경 메서드를 부르면 트랜잭션 밖에서 상태가 바뀌고 저장이 누락된다. 엔티티는 도메인 서비스의 트랜잭션이 끝나면 준영속이 되므로 api 에서 바꿔도 저장되지 않는다.

## 이렇게 한다

```java
@PostMapping("/{orderId}/cancel")
public OrderResponse cancel(@PathVariable Long orderId) {
    return OrderResponse.from(orderWriter.cancel(orderId));
}
```

## 이렇게 하지 않는다

```java
@PostMapping("/{orderId}/cancel")
public OrderResponse cancel(@PathVariable Long orderId) {
    Order order = orderReader.find(orderId);
    order.cancel();                                   // 트랜잭션 밖 상태 변경, 저장 안 됨
    return OrderResponse.from(order);
}
```

## 고치는 법

- 상태 변경은 `*Writer` 메서드로 옮기고 api 는 그 메서드 하나를 부른다.
- 판단이 필요하면 애그리거트·`*Policy` 가 하도록 도메인 서비스나 usecase 에 둔다.
