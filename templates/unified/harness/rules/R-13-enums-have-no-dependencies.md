---
id: R-13
title: core-enum 의 enum 은 아무것도 의존하지 않는다
kind: 구조
applies_to: com.meteor.enums (core/core-enum)
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R13_enums_have_no_dependencies]
related: [R-12]
human_doc: docs/architecture/structure.md#R-13
---

# R-13 core-enum 의 enum 은 아무것도 의존하지 않는다

## 규칙

`com.meteor.enums` 의 enum 은 다른 `com.meteor` 패키지, Spring, `jakarta` 에 의존하지 않는다. 상태와 종류의 이름만 가진다. 전이 규칙은 애그리거트에 둔다.

## 왜

모든 계층이 같은 enum 을 쓰므로, enum 에 의존이 생기면 전 모듈로 전파된다. api DTO 가 도메인 타입 없이(R-12) 상태를 표현할 수 있는 것이 이 모듈 덕분이다.

## 이렇게 한다

```java
public enum OrderStatus {
    CREATED, PAID, CANCELLED
}
```

## 이렇게 하지 않는다

```java
public enum OrderStatus {
    CREATED, PAID, CANCELLED;
    public boolean canCancel() { return this == CREATED; }   // 전이 규칙은 Order 에
}
```

## 고치는 법

- 상태에 따른 판단은 애그리거트 메서드(`Order.cancel`, `Order.payableAmount`)로 옮긴다.
