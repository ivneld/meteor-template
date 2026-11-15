---
id: R-16
title: 애그리거트를 바꾼 *Writer 메서드는 save 를 부른다
kind: 구조
applies_to: com.meteor.domain 의 *Writer 메서드
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R16_writers_save_what_they_change]
related: [R-04, R-15, R-17]
human_doc: docs/architecture/domain-services.md#R-16
---

# R-16 애그리거트를 바꾼 *Writer 메서드는 save 를 부른다

## 규칙

`*Writer` 메서드가 애그리거트의 상태 변경 메서드(정적 메서드와 접근자가 아닌 인스턴스 메서드)를 부르면, 같은 메서드 안에서 `*Repository.save*` 를 부르고 그 결과를 돌려준다. 이전의 의미 규칙 S-05 를 구조 규칙으로 옮긴 것이다.

## 왜

분리형은 더티 체킹이 없다. 애그리거트는 순수 객체라 `save()` 를 부르지 않으면 변경이 그대로 사라진다. 조용히 사라지는 데이터라 리뷰에 맡기지 않고 빌드에서 막는다.

## 이렇게 한다

```java
public Order cancel(Long orderId) {
    Order order = load(orderId);
    order.cancel();
    return orderRepository.save(order);
}
```

## 이렇게 하지 않는다

```java
public Order cancel(Long orderId) {
    Order order = load(orderId);
    order.cancel();
    return order;                 // save 없음
}
```

## 고치는 법

- 상태를 바꾼 메서드 끝에서 `xxxRepository.save(aggregate)` 의 결과를 돌려준다.

## 판별 한계

판별은 메서드 단위다. 상태 변경을 private 헬퍼로 나누면 그 헬퍼 안에서 save 까지 해야 통과한다. 접근자 판별은 이름(`get*`, `is*`, `has*`, `*Amount`)으로 한다. 상태를 바꾸는 메서드에 이런 이름을 붙이지 않는다.
