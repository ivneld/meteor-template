# 구조

모듈, 패키지, 계층과 그 사이의 의존 방향을 설명한다.

## 모듈

```
core-enum       com.meteor.enums. 모든 도메인 enum. 어떤 모듈·라이브러리에도 의존하지 않는다
core-api        유일한 실행 모듈. com.meteor.domain / usecase / api + support(error, web, async).
                애그리거트(JPA 엔티티)와 *Repository(Spring Data 인터페이스)가 domain 에 있다
storage/db-core support.storage(DataSource, JPA 설정, BaseEntity). 저장소 인프라만 있고 엔티티는 없다
clients/*       외부 시스템 어댑터. com.meteor.client.<이름>. 예: client-pg
support/*       logging, monitoring
tests/api-docs  REST Docs 테스트 베이스
```

```
core-api ──► storage/db-core       (JPA, BaseEntity)
   ├──► core-enum
   └──► clients/*
```

모듈 의존은 한 방향이다. core-api 는 storage/db-core 에서 JPA 와 `BaseEntity` 를 받는다. storage 와 clients 가 core-api 를 모른다는 것은 Gradle 이 컴파일
단계에서 막는다. 모듈로 막을 수 없는 세부 규칙은 ArchUnit(`ArchitectureRules`)이 `./gradlew test` 에서 잡는다.

## 계층과 의존 방향

```
 api ──────────► usecase ──────────► domain  (애그리거트 = 엔티티, *Repository = Spring Data)
  │                 │                  ▲
  │                 └──► clients       │
  └────────────────────────────────────┘
     (애그리거트 하나로 끝나는 흐름은 api 가 *Reader / *Writer 를 바로 부른다)

 모든 계층 ──► enums (core-enum)
 domain ──► support.error, support.storage.BaseEntity
```

애그리거트가 곧 엔티티라 엔티티 ↔ 애그리거트 변환이 없다. 상태는 core-enum 의 enum 을 `@Enumerated(STRING)` 컬럼으로, 값 객체는
`@Embeddable` record 를 컬럼으로 펼쳐 저장한다.

<a id="R-02"></a>
### R-02 의존은 바깥에서 안쪽으로만 흐른다

usecase 는 api 를 모른다. 저장소 인프라(`support.storage`)와 client 는 domain, usecase, api 를 모른다.

의존이 거꾸로 흐르면 안쪽 계층을 바꿀 때 바깥 계층 때문에 막힌다. 저장소 인프라가 도메인을 모르는 것은 모듈 의존이 1차로 막고 이 규칙이
다시 확인한다. `CoreJpaConfig` 는 `com.meteor` 를 문자열로 스캔할 뿐 도메인 타입을 참조하지 않는다. client 가 도메인 타입을 받기 시작하면 외부 연동 방식이 도메인 모델을 따라 바뀌어야 한다. client 는 자기 요청·응답 타입
(`PgApproveRequest`, `PgApproval`)만 쓰고, 도메인과의 변환은 usecase 가 한다.

강제: ArchUnit `R02_usecase_does_not_know_api`, `R02_adapters_do_not_know_core`.

## 패키지 규약

```
com.meteor.enums              모든 도메인 enum (상태, 종류)                                                        (core-enum)
com.meteor.domain             애그리거트(@Entity), 값 객체(@Embeddable record), *Policy  ← 도메인 모델            (core-api)
                              *Reader, *Writer                               ← 도메인 서비스. Spring 빈
                              *Repository                                    ← Spring Data 인터페이스
com.meteor.usecase            *UseCase, *Command, *Result                                                        (core-api)
com.meteor.api                *Controller                                                                        (core-api)
  api.request / api.response  *Request / *Response
com.meteor.client.<이름>      외부 연동 어댑터                                                                    (clients/*)
com.meteor.support            어느 계층에도 속하지 않는 공통 영역
  support.error                 ErrorCode, CoreException, LogLevel. 프레임워크를 모른다                          (core-api)
  support.web / support.async   어드바이스, ProblemDetails, 설정                                                  (core-api)
  support.storage               DataSource/JPA 설정, BaseEntity. 애그리거트가 BaseEntity 를 상속한다             (storage/db-core)
```

<a id="R-03"></a>
### R-03 이름은 닫힌 어휘에서만 고른다

`*Service` 접미어는 쓰지 않는다. domain 의 Spring 빈은 `*Reader`, `*Writer`, `*Repository` 뿐이다. usecase 의 최상위 클래스는 `*UseCase`,
`*Command`, `*Result` 중 하나다. api 의 최상위 클래스는 `*Controller` 이고, `api.request` 에는 `*Request`, `api.response` 에는
`*Response` 만 둔다.

이름만으로 역할이 드러나야 규칙과 흐름과 트랜잭션이 한 클래스에 섞이지 않는다. `*Service`, `*Manager`, `*Handler`, `*Processor`,
`*Helper` 는 무엇을 하는 클래스인지 드러내지 못하므로 쓰기 시작하면 아무 일이나 그 클래스로 모인다. `*Reader` 와 `*Writer` 는 이름이 곧
트랜잭션 속성이라 R-07 로 그대로 검사할 수 있다.

```
domain   OrderReader, OrderWriter, OrderRepository     (빈)
         Order, Money, OrderLimitPolicy                (모델)
usecase  OrderUseCase, OrderPlaceCommand
api      OrderController, request.OrderPlaceRequest, response.OrderResponse
```

`support.web.HealthController` 는 support 에 있어 대상이 아니다.

강제: ArchUnit `R03_no_service_suffix`, `R03_domain_beans_are_readers_writers_or_repositories`,
`R03_usecase_classes_are_use_cases_commands_or_results`, `R03_api_classes_are_controllers`, `R03_api_requests_are_named_request`,
`R03_api_responses_are_named_response`.

## 계층별 책임

| 계층 | 하는 일 | 하지 않는 것 |
|---|---|---|
| `api` | HTTP 요청 → Command 나 도메인 서비스 인자로 변환, 애그리거트 → 응답 DTO 변환. 엔드포인트 하나는 `*UseCase` 나 `*Reader`/`*Writer` 의 메서드 하나만 호출 | `@Transactional`, `*Repository`·`*Policy`·저장소 인프라·client 접근, 애그리거트의 상태 변경 메서드 호출, 값 객체 연산, DTO 필드에 도메인 타입 |
| `usecase` | 여러 도메인 서비스를 엮는 흐름, 외부 시스템 호출, 트랜잭션 경계 조절(묶기, 나누기, `REQUIRES_NEW`) | 규칙 판단, 도메인 상태로 분기, `*Repository` 직접 사용, 다른 `*UseCase` 호출 |
| `domain` 도메인 서비스 | 애그리거트 하나의 조회(`*Reader`)와 변경(`*Writer`). 꺼내고, 애그리거트·Policy 에 넘기고, 저장한다 | 다른 애그리거트의 `*Repository`, 다른 도메인 서비스, 외부 시스템, usecase·api 타입, 기본 전파 외의 트랜잭션 설정 |
| `domain` 도메인 모델 | 모든 규칙과 상태 전이. 애그리거트(엔티티), 값 객체, `*Policy`. JPA 매핑 어노테이션을 갖는다 | Spring, Hibernate 전용 API, 저장소, 도메인 서비스, setter, 연관관계 |
| `domain` 저장소 | `*Repository` 는 Spring Data 인터페이스. 조회 조건을 인자로 받는 쿼리 메서드 | 규칙이 박힌 쿼리, `EntityManager` 직접 사용 |
| `enums` | 상태·종류 enum. 모든 계층이 같은 타입을 쓴다 | 다른 모듈 의존, 로직 |
| `support.storage` / `clients` | DataSource·JPA 설정·BaseEntity, 외부 연동 | 도메인 참조, 비즈니스 규칙, 트랜잭션 경계 |

## 값 타입의 자리

```
새 타입이 생겼다
  │
  ├─ 식별자가 있고 상태가 바뀐다 ──▶ 애그리거트 (com.meteor.domain, @Entity, BaseEntity 상속)
  │
  ├─ enum 이다 ──▶ com.meteor.enums (core-enum)     예: OrderStatus, PaymentMethod
  │                api 의 요청·응답도 같은 타입을 쓰므로 모듈이 따로 있다
  │
  └─ record 다 ──▶ com.meteor.domain (@Embeddable)  예: Money, Address, Email
```

<a id="R-13"></a>
### R-13 enum 은 의존이 없다

core-enum 의 enum 은 다른 패키지와 프레임워크에 의존하지 않는다.

enum 은 모든 계층이 참조하는 공용 어휘다. API 계약에도 그대로 쓰인다. 여기에 의존이 생기면 그 의존이 모든 모듈로 전파된다. 상태 전이 같은 규칙은 enum 에
두지 않고 애그리거트에 둔다. enum 은 값의 목록일 뿐이다.

강제: ArchUnit `R13_enums_have_no_dependencies`.
