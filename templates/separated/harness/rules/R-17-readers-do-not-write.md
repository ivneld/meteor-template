---
id: R-17
title: *Reader 는 쓰지 않는다
kind: 구조
applies_to: com.meteor.domain 의 *Reader
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R17_readers_do_not_write]
related: [R-07, R-16]
human_doc: docs/architecture/domain-services.md#R-17
---

# R-17 *Reader 는 쓰지 않는다

## 규칙

`*Reader` 는 `*Repository` 의 `save*`, `delete*`, `flush*` 를 부르지 않는다. 쓰기는 같은 애그리거트의 `*Writer` 가 한다.

## 왜

`*Reader` 는 readOnly 트랜잭션이다(R-07). 그 안의 쓰기는 플러시되지 않거나 드라이버에 따라 에러가 난다. 이름이 곧 트랜잭션 속성이라는 약속이 깨지면 R-08 같은 이름 기반 규칙도 믿을 수 없게 된다.

## 이렇게 한다

```java
public Money payableAmount(Long orderId) {
    return find(orderId).payableAmount();      // 읽고 판단만
}
```

## 이렇게 하지 않는다

```java
public Order find(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow(...);
    return orderRepository.save(order);        // Reader 가 저장
}
```

## 고치는 법

- 쓰기가 섞인 메서드를 `*Writer` 로 옮긴다. 조회 후 쓰기를 한 흐름으로 묶어야 하면 `*Writer` 메서드 하나로 만든다.
