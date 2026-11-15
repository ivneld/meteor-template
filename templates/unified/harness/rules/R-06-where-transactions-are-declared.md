---
id: R-06
title: @Transactional 은 usecase 와 *Reader / *Writer 에만 있다
kind: 구조
applies_to: com.meteor 전체
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R06_transactional_classes_only_in_usecase_or_domain_services, R06_transactional_methods_only_in_usecase_or_domain_services, R06_programmatic_transactions_only_in_usecase]
related: [R-07, R-08, R-18]
human_doc: docs/architecture/transactions.md#R-06
---

# R-06 @Transactional 은 usecase 와 *Reader / *Writer 에만 있다

## 규칙

- `@Transactional`(클래스·메서드)은 `com.meteor.usecase` 와 domain 의 `*Reader`·`*Writer` 에만 붙는다.
- `TransactionTemplate`, `TransactionManager` 같은 프로그래밍 방식 트랜잭션은 usecase 에만 있다.
- api, `*Repository`, `*Policy`, 애그리거트, client, support 에는 트랜잭션이 없다.

## 왜

트랜잭션을 여는 곳이 두 종류뿐이어야 "이 코드가 어느 트랜잭션 안에서 도는가" 를 호출 체인을 따라가지 않고 알 수 있다. 컨트롤러에 트랜잭션이 생기면 흐름 조율이 api 로 새고(R-11), 어댑터에 생기면 경계가 저장소로 숨는다.

## 이렇게 한다

```java
@Service
public class OrderUseCase {
    @Transactional
    public Order place(OrderPlaceCommand command) { ... }
}
```

## 이렇게 하지 않는다

```java
@RestController
public class OrderController {
    @Transactional                    // api 는 트랜잭션을 열지 않는다
    @PostMapping
    public OrderResponse place(...) { ... }
}
```

## 고치는 법

- 여러 호출을 한 트랜잭션으로 묶어야 하면 usecase 메서드를 만들어 거기에 `@Transactional` 을 붙인다.
- 구간을 나눠야 하면 usecase 에서 `TransactionTemplate` 을 쓴다(playbooks/split-transaction.md).
