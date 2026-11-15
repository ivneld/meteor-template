---
id: R-03
title: 클래스 이름은 닫힌 어휘에서 고른다
kind: 구조
applies_to: com.meteor 전체. 특히 domain, usecase, api, api.request, api.response
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R03_no_service_suffix, R03_domain_beans_are_readers_writers_or_repositories, R03_usecase_classes_are_use_cases_commands_or_results, R03_api_classes_are_controllers, R03_api_requests_are_named_request, R03_api_responses_are_named_response]
related: [R-04, R-06]
human_doc: docs/architecture/structure.md#R-03
---

# R-03 클래스 이름은 닫힌 어휘에서 고른다

## 규칙

| 위치 | 허용 이름 |
|---|---|
| 어디서든 | `*Service` 금지 |
| `com.meteor.domain` 의 Spring 빈 | `*Reader`, `*Writer`, `*Repository` |
| `com.meteor.domain` 의 그 외 | 애그리거트·값 객체는 도메인 명사, 여러 애그리거트 규칙은 `*Policy` |
| `com.meteor.usecase` 최상위 클래스 | `*UseCase`, `*Command`, `*Result` |
| `com.meteor.api` 최상위 클래스 (request·response 제외) | `*Controller` |
| `com.meteor.api.request` | `*Request` |
| `com.meteor.api.response` | `*Response` |

`*Manager`, `*Handler`, `*Processor`, `*Helper`, `*Util` 처럼 역할이 드러나지 않는 이름은 domain·usecase·api 에 만들지 않는다. `support.web.HealthController` 처럼 support 에 있는 것은 대상이 아니다.

## 왜

이름이 곧 역할과 트랜잭션 속성이다(`*Reader` = readOnly, `*Writer` = 쓰기, `*UseCase` = 경계 조절). 이름이 닫혀 있어야 규칙과 흐름과 트랜잭션이 한 클래스에 섞이지 않고, R-04·R-07·R-08 같은 규칙을 이름으로 검사할 수 있다.

## 이렇게 한다

```java
public class OrderWriter { ... }       // domain, 주문 생성·변경
public class OrderUseCase { ... }      // usecase, 회원 확인 + 주문 생성
public record OrderPlaceCommand(...)    // usecase 입력
```

## 이렇게 하지 않는다

```java
public class OrderService { ... }      // 무엇을 하는지 드러나지 않는다
public class OrderCanceller { ... }    // 동사 단위 쪼개기. OrderWriter.cancel 로 둔다
```

## 고치는 법

- 조회면 `*Reader`, 생성·변경이면 `*Writer` 로 이름을 바꾼다. 둘 다라면 나눈다.
- 여러 도메인 서비스를 엮거나 외부를 부르면 `*UseCase` 로 usecase 에 둔다.
- `*Writer` 가 커지면 먼저 규칙이 새지 않았는지(S-04) 보고, 그래도 크면 `*Writer` 둘로 나누되 이름은 `*Writer` 로 끝낸다.
