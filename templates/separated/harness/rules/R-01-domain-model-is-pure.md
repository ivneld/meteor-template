---
id: R-01
title: 도메인 모델은 프레임워크를 모른다
kind: 구조
applies_to: com.meteor.domain 의 애그리거트, 값 객체(record), *Policy. domain 전체. com.meteor.support.error
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R01_domain_model_is_pure, R01_domain_knows_no_outer_layers, R01_domain_uses_only_error_vocabulary_from_support, R01_error_vocabulary_knows_no_framework]
related: [R-04, R-05, R-13]
human_doc: docs/architecture/domain-model.md#R-01
---

# R-01 도메인 모델은 프레임워크를 모른다

## 규칙

- 애그리거트, 값 객체, `*Policy` 는 Spring, JPA(`jakarta.persistence`), `com.meteor.storage`, client, usecase, api, `*Repository`, `*Reader`, `*Writer` 에 의존하지 않는다.
- `com.meteor.domain` 전체(도메인 서비스 포함)는 usecase, api, client 를 모른다.
- domain 이 `com.meteor.support` 에서 쓰는 것은 `support.error`(ErrorCode, CoreException)뿐이다.
- `support.error` 는 프레임워크와 다른 패키지를 모른다.

## 왜

규칙이 프레임워크와 분리되어야 목 없이 단위 테스트할 수 있다(`OrderTest`, `OrderLimitPolicyTest`). 도메인 서비스가 외부 시스템과 상위 계층 타입을 모르면 범위가 커지지 않는다. 배치 같은 두 번째 실행 모듈이 생기면 `domain` 과 `support.error` 를 파일 이동만으로 core-domain 모듈로 뺄 수 있다.

## 이렇게 한다

```java
// Order: 순수 자바. 규칙과 상태 전이만 있다
public class Order {
    private final Long memberId;
    private OrderStatus status;
    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", id);
        }
        status = OrderStatus.CANCELLED;
    }
}
```

## 이렇게 하지 않는다

```java
@Entity                                    // JPA 는 storage 의 OrderEntity 에만
public class Order {
    @Autowired OrderRepository repository;  // 도메인 모델은 빈을 모른다
}
```

## 고치는 법

- 저장소가 필요한 판단이면 사실(건수, 금액)을 `*Writer`/`*Reader` 가 꺼내 넘기고 판단은 애그리거트나 `*Policy` 가 한다(S-04).
- 외부 시스템이 필요하면 usecase 로 올린다.
- 도메인이 쓸 에러는 `ErrorCode` 에 추가하고 `CoreException` 으로 던진다.
