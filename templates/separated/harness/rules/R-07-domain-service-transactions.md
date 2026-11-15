---
id: R-07
title: *Reader 는 readOnly, *Writer 는 쓰기 트랜잭션이고 기본 전파만 쓴다
kind: 구조
applies_to: com.meteor.domain 의 *Reader, *Writer
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R07_readers_are_read_only, R07_writers_are_read_write, R07_domain_services_use_default_propagation]
related: [R-06, R-08, R-17]
human_doc: docs/architecture/transactions.md#R-07
---

# R-07 *Reader 는 readOnly, *Writer 는 쓰기 트랜잭션이고 기본 전파만 쓴다

## 규칙

- `*Reader` 는 클래스에 `@Transactional(readOnly = true)` 를 붙인다.
- `*Writer` 는 클래스에 `@Transactional` 을 붙인다.
- 메서드에 다시 붙인다면 기본 전파(`REQUIRED`)만 쓴다. `REQUIRES_NEW`, `NOT_SUPPORTED` 등은 usecase 에서만 쓴다.

## 왜

도메인 서비스는 혼자 불리면 독립 트랜잭션이 되고, usecase 가 연 트랜잭션 안에서 불리면 거기에 참여한다. 그래서 api 가 바로 불러도, usecase 가 묶어도 같은 코드가 맞게 동작한다. 경계를 조절하는 일은 usecase 한 곳에서만 한다. 바깥 쓰기 트랜잭션에 참여하면 `*Reader` 의 readOnly 는 무시되는데, 이것은 의도된 동작이다.

## 이렇게 한다

```java
@Component
@Transactional(readOnly = true)
public class OrderReader { ... }

@Component
@Transactional
public class OrderWriter { ... }
```

## 이렇게 하지 않는다

```java
@Component
@Transactional
public class PaymentWriter {
    @Transactional(propagation = Propagation.REQUIRES_NEW)   // 경계 조절은 usecase 만
    public Payment fail(Long paymentId) { ... }
}
```

## 고치는 법

- 별도 트랜잭션이 필요하면 usecase 에서 `TransactionTemplate` 구간으로 나눈다. 바깥 트랜잭션 안에서 `REQUIRES_NEW` 로 여는 방식은 커넥션을 둘 쥐므로 피한다(transactions.md 의 나누기).
