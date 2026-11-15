---
id: R-15
title: 도메인 모델은 public setter 를 갖지 않는다
kind: 구조
applies_to: com.meteor.domain 의 애그리거트, 값 객체, *Policy
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R15_domain_model_has_no_setters]
related: [R-16, R-20, S-02]
human_doc: docs/architecture/domain-model.md#R-15
---

# R-15 도메인 모델은 public setter 를 갖지 않는다

## 규칙

도메인 모델에 `public void setXxx(...)` 를 만들지 않는다. 상태는 규칙을 가진 메서드(`cancel`, `markPaid`, `approve`, `withdraw`)로만 바뀐다.

## 왜

setter 가 있으면 규칙이 서비스로 새어 나가고(빈약한 도메인), 같은 전이를 여러 곳에서 다르게 하게 된다. JPA 는 필드 접근이라 setter 가 필요 없다. 매핑 편의로 setter 를 만들고 싶어진다면 S-09 의 신호다.

## 이렇게 한다

```java
public void markPaid() {
    ensurePayable();
    status = OrderStatus.PAID;
}
```

## 이렇게 하지 않는다

```java
public void setStatus(OrderStatus status) { this.status = status; }
// OrderWriter: if (order.getStatus() == CREATED) order.setStatus(PAID);
```

## 고치는 법

- setter 를 지우고, 그 setter 를 부르던 곳의 조건과 대입을 의미 있는 이름의 애그리거트 메서드로 옮긴다. 그 메서드의 단위 테스트를 같이 쓴다.
