---
id: R-21
title: 애그리거트의 restore 는 같은 이름의 *Repository 만 부른다
kind: 구조
applies_to: Order.restore, Member.restore, Payment.restore 호출
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R21_only_own_repository_restores_aggregates]
related: [R-05, R-20, S-02]
human_doc: docs/architecture/domain-model.md#R-21
---

# R-21 애그리거트의 restore 는 같은 이름의 *Repository 만 부른다

## 규칙

`Xxx.restore(...)` 는 `XxxRepository`(저장소 어댑터)의 `toDomain` 에서만 부른다. 도메인 서비스, usecase, 다른 어댑터는 부르지 않는다.

## 왜

`restore` 는 저장소에서 읽은 상태를 검사 없이 되살리는 통로다. 도메인 서비스가 `restore` 로 새 상태(예: status 를 PAID 로)를 조립하면 애그리거트의 전이 규칙을 우회한다. 이전 S-02 의 신호 중 구조로 판별되는 부분이다.

## 이렇게 한다

```java
private static Order toDomain(OrderEntity entity) {
    return Order.restore(entity.getId(), entity.getMemberId(), ...);
}
```

## 이렇게 하지 않는다

```java
// OrderWriter
Order paid = Order.restore(order.getId(), order.getMemberId(), ..., OrderStatus.PAID);
return orderRepository.save(paid);
```

## 고치는 법

- 상태 전이를 애그리거트 메서드(`markPaid`)로 하고 `save` 한다.
- 테스트에서 특정 상태의 애그리거트가 필요하면 테스트 코드에서 `restore` 를 써도 된다(테스트는 검사 대상이 아니다).
