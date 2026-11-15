---
id: R-11
title: 엔드포인트 하나는 usecase 나 도메인 서비스 메서드 하나만 부른다
kind: 구조
applies_to: com.meteor.api 의 메서드
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R11_endpoint_calls_one_use_case_or_domain_service]
related: [R-06, R-09, R-10]
human_doc: docs/architecture/api.md#R-11
---

# R-11 엔드포인트 하나는 usecase 나 도메인 서비스 메서드 하나만 부른다

## 규칙

컨트롤러 메서드 하나는 `*UseCase`·`*Reader`·`*Writer` 의 메서드를 최대 하나 부른다.

## 왜

여러 개를 부르면 호출마다 트랜잭션이 따로 열려 부분 커밋이 생기고, 흐름을 컨트롤러가 조율하게 된다. 묶어야 하는 흐름은 이름을 가진 usecase 메서드여야 테스트하고 재사용할 수 있다.

## 이렇게 한다

```java
@PostMapping
public OrderResponse place(@RequestBody @Valid OrderPlaceRequest request) {
    return OrderResponse.from(orderUseCase.place(request.toCommand()));
}
```

## 이렇게 하지 않는다

```java
@PostMapping
public OrderResponse place(@RequestBody @Valid OrderPlaceRequest request) {
    memberReader.ensureActive(request.memberId());          // 호출 1, 트랜잭션 1
    return OrderResponse.from(orderWriter.place(...));      // 호출 2, 트랜잭션 2
}
```

## 고치는 법

- 여러 호출을 usecase 메서드 하나로 옮기고(playbooks/new-use-case.md), 쓰기가 섞이면 `@Transactional` 을 붙인다(R-08).
