---
id: R-09
title: usecase 는 도메인 서비스를 거치고 다른 usecase 를 부르지 않는다
kind: 구조
applies_to: com.meteor.usecase
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R09_use_cases_go_through_domain_services, R09_use_cases_do_not_call_each_other]
related: [R-04, R-08, R-11]
human_doc: docs/architecture/domain-services.md#R-09
---

# R-09 usecase 는 도메인 서비스를 거치고 다른 usecase 를 부르지 않는다

## 규칙

- usecase 는 `*Repository` 를 쓰지 않는다. 저장소는 `*Reader`·`*Writer` 를 거친다.
- `*UseCase` 는 다른 `*UseCase` 를 부르지 않는다.

## 왜

저장소 접근이 도메인 서비스 한 곳에 모여야 "없으면 404" 같은 공통 처리와 저장 규칙(R-16)이 한 곳에 있다. usecase 끼리 부르면 트랜잭션 경계가 중첩되어, 어디서 열리고 어디에 합류하는지 한 클래스만 보고 알 수 없다.

## 이렇게 한다

```java
// 반복되는 흐름은 이름을 붙인 usecase 메서드 하나로 만든다
@Transactional
public Order place(OrderPlaceCommand command) {
    memberReader.ensureActive(command.memberId());
    return orderWriter.place(...);
}
```

## 이렇게 하지 않는다

```java
public Payment pay(PaymentPayCommand command) {
    Order order = orderUseCase.place(...);          // usecase 가 usecase 를
    orderRepository.findById(command.orderId());    // usecase 가 저장소를 직접
}
```

## 고치는 법

- 저장소 호출은 해당 애그리거트의 `*Reader`·`*Writer` 메서드로 옮긴다.
- 다른 usecase 의 흐름이 필요하면 그 흐름에 필요한 도메인 서비스를 직접 엮는다. 몇 줄의 흐름이 usecase 마다 반복되는 것은 괜찮다.
