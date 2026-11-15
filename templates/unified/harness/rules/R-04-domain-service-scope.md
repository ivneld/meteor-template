---
id: R-04
title: 도메인 서비스는 자기 애그리거트의 *Repository 만 쓴다
kind: 구조
applies_to: com.meteor.domain 의 *Reader, *Writer
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R04_domain_services_touch_only_their_own_repository]
related: [R-09, S-04, S-01]
human_doc: docs/architecture/domain-services.md#R-04
---

# R-04 도메인 서비스는 자기 애그리거트의 *Repository 만 쓴다

## 규칙

`XxxReader`·`XxxWriter` 는 `XxxRepository`(Spring Data 인터페이스)만 쓴다. 다른 `*Repository`, 다른 `*Reader`·`*Writer` 를 쓰지 않는다.

## 왜

도메인 서비스가 비대해지는 길은 대부분 다른 애그리거트의 저장소나 다른 도메인 서비스를 부르는 데서 열린다. 하나가 애그리거트 하나만 다루면 크기가 애그리거트에 묶이고, 여러 애그리거트를 엮는 흐름은 usecase 한 곳에 모인다.

## 이렇게 한다

```java
// 다른 애그리거트의 값은 usecase 가 받아 인자로 넘긴다
Payment requested = transaction.execute(status -> paymentWriter.request(command.orderId(),
        orderReader.payableAmount(command.orderId()), command.method()));
```

## 이렇게 하지 않는다

```java
@Component
@Transactional
public class PaymentWriter {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;   // 다른 애그리거트의 저장소
    private final OrderReader orderReader;           // 다른 도메인 서비스
}
```

## 고치는 법

- 다른 애그리거트의 값 하나가 필요하면 usecase 가 상대 `*Reader` 에서 받아 인자로 넘긴다.
- 두 애그리거트를 함께 바꿔야 하면 usecase 메서드에 `@Transactional` 을 붙이고 각 `*Writer` 를 부른다(R-08).
- 다른 애그리거트에 걸친 판단이면 사실을 모아 `*Policy` 에 넘긴다(S-04).
