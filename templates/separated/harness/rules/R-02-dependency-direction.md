---
id: R-02
title: 의존은 바깥에서 안쪽으로만 흐른다
kind: 구조
applies_to: com.meteor.usecase, com.meteor.storage, com.meteor.client
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R02_usecase_does_not_know_api, R02_adapters_do_not_know_core]
related: [R-01, R-09]
human_doc: docs/architecture/structure.md#R-02
---

# R-02 의존은 바깥에서 안쪽으로만 흐른다

## 규칙

- usecase 는 api 를 모른다.
- storage(`com.meteor.storage`)와 client(`com.meteor.client.*`)는 domain, usecase, api 를 모른다. 모듈 의존(storage/db-core, clients/* 는 core-api 를 모른다)이 컴파일에서 1차로 막고 이 규칙이 다시 확인한다.

## 왜

의존이 한 방향이어야 바깥 계층(HTTP, 외부 API, 저장소)을 바꿔도 안쪽 규칙이 흔들리지 않는다. client 가 도메인 타입을 알면 외부 계약이 도메인 모델에 묶인다.

## 이렇게 한다

```java
// client 는 자기 타입만 쓰고, usecase 가 도메인 타입으로 바꾼다
PgApproval approval = pgClient.approve(requested.paymentKey(), requested.getAmount().amount());
```

## 이렇게 하지 않는다

```java
// client 가 도메인 애그리거트를 받는다
public PgApproval approve(Payment payment) { ... }
```

## 고치는 법

- client 는 원시 타입과 자기 record(`PgApproveRequest`, `PgApproval`)로 계약을 정하고, 변환은 usecase 가 한다.
- 실패는 client 자기 예외(`PgApprovalException`)로 던지고 usecase 가 `ErrorCode` 로 바꾼다.
