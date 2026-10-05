# meteor-template

Spring Boot(Java) 서비스를 팀이 함께 개발하기 위한 멀티모듈 템플릿. 단일 애플리케이션으로 시작하되,
컨텍스트 경계를 첫날부터 코드와 테스트로 고정해 나중에 구조를 키울 때 경계를 다시 찾지 않아도 되게 한다.

## 목차

- [개요](#개요)
- [아키텍처 확장 가이드](#아키텍처-확장-가이드)
  - [트리거와 대응](#트리거와-대응)
  - [확장 프로세스](#확장-프로세스)
  - [처음부터 하지 않는 것](#처음부터-하지-않는-것)
- [모듈 구성](#모듈-구성)
- [계층과 규칙](#계층과-규칙)
- [응답 포맷과 에러 코드](#응답-포맷과-에러-코드)
  - [구조](#구조)
  - [에러 코드 목록](#에러-코드-목록)
  - [신규 에러 코드 추가하기](#신규-에러-코드-추가하기)
  - [사용하기](#사용하기)
- [새 컨텍스트 추가하기](#새-컨텍스트-추가하기)
- [의존성 버전 관리](#의존성-버전-관리)
- [런타임 프로필](#런타임-프로필)
- [테스트 태스크와 태그](#테스트-태스크와-태그)
- [실행](#실행)
- [권장 IDE 설정](#권장-ide-설정-intellij-idea)
- [관련 문서](#관련-문서)

## 개요

이 템플릿은 **하나의 배포 단위, 여러 개의 바운디드 컨텍스트**를 전제로 한다. 코드는 컨텍스트(`sample`, `order`, `shipping` ...)
단위로 패키징되고, 컨텍스트 안은 `domain / application / api / storage` 네 계층으로 나뉜다. 컨텍스트끼리는 직접 참조하지 않고
`*Facade` 와 `*Event` 로만 협력한다. 이 규칙들은 ArchUnit 테스트와 리뷰 에이전트가 강제한다.

이렇게 시작하면 다음이 가능하다.

- 도메인 규칙은 `core-domain` 한 곳에만 있어 Spring 없이 단위 테스트된다.
- 트랜잭션 경계는 `application` 한 곳에만 있어 "어느 트랜잭션 안인가"가 항상 명확하다.
- 트래픽이나 조직이 커졌을 때 컨텍스트 하나를 떼어내는 작업이 "통신 방식 교체 + 테이블 이전"으로 줄어든다.

## 아키텍처 확장 가이드

구조는 필요가 생겼을 때 한 단계씩 키운다. 어떤 상황이 어떤 단계를 요구하는지, 그리고 그 순서가 아래에 있다.

### 트리거와 대응

| 트리거 | 하지 말아야 할 것 | 해야 할 것 |
|---|---|---|
| 새 기능·도메인이 생겼다 (예: 결제 추가) | 새 애플리케이션을 만든다 | 같은 앱에 새 컨텍스트 패키지를 추가한다. [새 컨텍스트 추가하기](#새-컨텍스트-추가하기) |
| 전체 요청이 늘어 응답이 느리다 | 코드를 나눈다 | 인스턴스를 늘린다. 앱은 무상태이므로 코드 변경이 없다. 먼저 병목이 앱인지 DB인지 외부 API인지 지표로 확인한다 |
| 조회 API 가 무겁다 | 인스턴스를 늘린다 | 컨텍스트별 조회 포트로 read model 을 돌려주고 캐시한다. 애그리거트를 통째로 올리지 않는다 |
| 특정 컨텍스트 트래픽이 다른 컨텍스트까지 느리게 한다 (noisy neighbor) | 서비스를 분리한다 | 1차: 해당 엔드포인트에 요청 제한·별도 스레드풀(bulkhead). 2차: 실행 모듈만 하나 더 만들어 그 컨텍스트의 UseCase·컨트롤러만 담고 게이트웨이에서 라우팅한다. 같은 저장소, 같은 DB, 같은 도메인 코드 |
| 컨텍스트 경계 규칙 위반이 반복된다 | 규칙을 느슨하게 한다 | 그 컨텍스트를 패키지에서 Gradle 모듈로 승격해 컴파일 단계에서 막는다 |
| 팀이 나뉘어 배포를 서로 기다린다 | 코드만 나눈다 | 서비스 분리. 배포 단위와 릴리스 주기를 독립시킨다 |
| DB 가 병목이라 특정 컨텍스트 데이터를 따로 둬야 한다 | 앱만 늘린다 | 서비스 분리. 컨텍스트 소유 테이블을 별도 DB 로 옮긴다. 실행 모듈 분리(DB 공유)로는 풀리지 않는다 |
| 가용성·규제 등급이 컨텍스트마다 다르다 (결제 PCI, 추천은 죽어도 됨) | 전체를 같은 등급으로 운영한다 | 서비스 분리 |
| 멀티테넌트 서비스다 | 나중에 넣는다 | **첫날에 결정한다.** 격리 방식(스키마/컬럼), 모든 쿼리의 테넌트 조건 강제, 테넌트 단위 감사. 이것은 MSA 전환보다 나중에 넣기 어렵다 |

트래픽, 코드 크기, 도메인 개수는 서비스 분리의 이유가 아니다. 각각 수평 확장, 모듈 경계, 컨텍스트 패키징으로 해결되며 전부 하나의 배포
단위 안에서 가능하다. 분리의 이유는 **조직(독립 배포)** 과 **데이터(저장소 격리)** 에서만 나온다.

### 확장 프로세스

1. **측정한다.** 병목이 앱 스레드인지, DB 커넥션인지, 외부 API 인지 지표로 확인한다. `support/monitoring` 이 이 단계에 쓰인다.
   지표 없이 확장하면 돈만 쓰고 끝난다.
2. **가장 싼 수단부터 쓴다.** 인스턴스 추가 → 캐시·read model → bulkhead → 실행 모듈 분리(DB 공유) → 서비스 분리(DB 분리).
   각 단계는 앞 단계로 해결되지 않는다는 근거가 있을 때만 간다.
3. **경계는 처음부터 지킨다.** 분리 비용은 그 시점의 코드가 아니라 그동안 규칙에 낸 예외의 개수로 결정된다.
   예외는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 의 절차대로 동결하고 ADR 로 남긴다.
4. **첫 분리 대상은 가장 독립적인 것을 고른다.** 알림, 정산, 검색처럼 다른 컨텍스트가 동기적으로 기다리지 않는 것. 첫 분리는
   브로커·게이트웨이·서비스 간 인증·분산 추적을 한꺼번에 세워야 해서 가장 비싸고, 두 번째부터는 그 비용이 없다.
   주문처럼 모두가 참조하는 중심 컨텍스트는 마지막이거나 영원히 남아도 된다.
5. **분리는 시작하면 끝낸다.** in-process 이벤트를 브로커로, `*Facade` 구현을 HTTP 클라이언트로 교체하고, 컨텍스트 소유 테이블을
   별도 DB 로 옮기는 것까지가 한 단위다. DB 를 공유한 채 멈추면 분산 모놀리스가 되어 양쪽의 단점만 남는다.
6. **다시 측정한다.** 분리가 해결하려던 지표가 실제로 바뀌었는지 본다.

### 처음부터 하지 않는 것

과도한 대비를 막는 선이다. 이 선을 넘는 순간 "대비"가 아니라 "선제 분리"가 된다.

- 메시지 브로커를 두지 않는다. 이벤트는 in-process `ApplicationEventPublisher`.
- 컨텍스트별 DB·스키마를 나누지 않는다. 테이블 접두어까지만.
- 컨텍스트별 Gradle 모듈로 쪼개지 않는다. 패키지 + ArchUnit 으로 충분하다. 위반이 반복될 때 올린다.
- 서비스 간 HTTP, 사가, 분산 추적 인프라를 두지 않는다.

## 모듈 구성

```
meteor-template
├── core
│   ├── core-shared     모든 계층이 공유하는 어휘. enum, VO, ErrorCode, CoreException. 외부 의존 0
│   ├── core-domain     애그리거트, 도메인 서비스, 리포지토리 포트. Spring/JPA 를 모름
│   └── core-api        유일한 실행 모듈. application(UseCase) + api(Controller) + support(설정, 어드바이스)
├── storage
│   └── db-core         도메인 포트의 JPA 구현. 엔티티는 이 안에서만 존재
├── clients
│   └── client-example  OpenFeign 기반 외부 연동 어댑터 예시
├── support
│   ├── logging         프로필별 logback, OpenTelemetry, Sentry
│   └── monitoring      Actuator + Prometheus
├── tests
│   └── api-docs        Spring REST Docs 테스트 베이스
└── docs
    ├── ARCHITECTURE.md 규칙의 단일 출처 (R-xx 구조 규칙, S-xx 의미 규칙)
    └── REVIEW_AGENT.md 리뷰 에이전트 하네스
```

### 의존 방향

```
        api ──► application ──► domain ◄── storage / clients
         │           │            ▲              │
         └───────────┴────────────┴──────────────┴──► core-shared
```

`core-domain` 은 아무것도 모른다. `storage` 와 `clients` 는 도메인 포트를 구현하므로 도메인에 의존한다(의존성 역전).
`core-api` 만 전부를 안다. 실행 모듈이 얇기 때문에 배치나 다른 API 모듈을 추가할 때 `core-domain` 과 `storage` 를 그대로 재사용한다.

### 패키지 규약

컨텍스트가 계층보다 위에 온다.

```
com.meteor.<context>.domain        애그리거트, 도메인 서비스(*Policy), *Repository 포트      (core-domain)
com.meteor.<context>.application   *UseCase, *Command, *Result, *Facade, *Event               (core-api)
com.meteor.<context>.api           *Controller, request/*, response/*                          (core-api)
com.meteor.<context>.storage       *Entity, *JpaRepository, *RepositoryAdapter                 (storage/db-core)
com.meteor.<context>.clients       외부 연동 어댑터                                             (clients/*)
com.meteor.shared                  공유 어휘                                                    (core-shared)
com.meteor.support                 컨텍스트에 속하지 않는 설정·어드바이스·공통 클라이언트
```

## 계층과 규칙

| 계층 | 하는 일 | 하지 않는 것 |
|---|---|---|
| `api` | HTTP ↔ Command/Result 변환. 엔드포인트 하나는 UseCase 메서드 하나 호출 | `@Transactional`, 도메인·저장소 접근, VO 연산 |
| `application` | 유스케이스 흐름. 꺼내고, 도메인 메서드를 부르고, 저장한다. `@Transactional` 의 유일한 자리 | 도메인 상태로 분기, 다른 UseCase 호출 |
| `domain` | 모든 규칙과 상태 전이. 식별자를 가진 가변 객체(애그리거트)와 불변 값(VO) | Spring, JPA, `*Service` 접미어 |
| `storage` / `clients` | 포트 구현. 엔티티 ↔ 도메인 변환 | 비즈니스 규칙, 트랜잭션 경계 |

핵심 규칙 세 줄.

- `domain` 은 아무것도 모른다 (R-01).
- `@Transactional` 은 `application` 에만 있다 (R-02).
- 컨텍스트끼리 직접 의존하지 않는다. `*Facade` 와 `*Event` 로만 (R-07).

전체 규칙과 이유, 예외 절차는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 에 있다. 구조 규칙(R-xx)은 `core-api` 의
`ArchitectureRules` 가 `./gradlew test` 에서 검사하고, 의미 규칙(S-xx)은 [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) 의
설정으로 리뷰 에이전트가 PR 마다 본다.

## 응답 포맷과 에러 코드

성공 응답은 리소스 DTO 를 그대로 내려준다. 오류 응답은 [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
(`application/problem+json`) 형식이며, `code` 확장 속성으로 에러 코드를 함께 전달한다.

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "urn:meteor:error:S001",
  "title": "Sample not found.",
  "status": 404,
  "instance": "/api/v1/samples/999",
  "code": "S001",
  "sampleId": 999
}
```

| 필드 | 출처 | 설명 |
|---|---|---|
| `type` | `ProblemDetails` | 에러 코드 URI. `urn:meteor:error:{code}`. 문서 URL 이 생기면 `ProblemDetails.TYPE_PREFIX` 만 바꾼다 |
| `title` | `ErrorCode` | 에러 종류의 제목. 같은 코드면 항상 같다 |
| `status` | `ErrorCode` | HTTP 상태. 응답 상태 라인과 항상 일치한다 |
| `detail` | `CoreException` | 발생 건에 대한 부가 설명. 없으면 생략된다 |
| `instance` | Spring MVC | 요청 경로. 자동으로 채워진다 |
| `code` | `ErrorCode` | 클라이언트가 분기에 사용하는 에러 코드 문자열 |
| 그 외 | `CoreException.property()` | 확장 속성. 최상위에 그대로 노출된다 |

### 구조

에러 코드는 어휘이고 ProblemDetail 은 표현이다. 그래서 둘은 다른 모듈에 있다.

```
core-shared  com.meteor.shared.error
  ErrorCode (enum)         코드 문자열 + HTTP 상태(int) + 제목 + 로그 레벨. fromStatus() 로 프레임워크 오류에 공통 코드 선택
  CoreException            어디서든 던질 수 있는 유일한 예외. errorCode + detail(선택) + properties
  LogLevel                 로깅 프레임워크에 의존하지 않기 위한 자체 enum

core-api     com.meteor.support.web
  ProblemDetails           ErrorCode / CoreException → ProblemDetail 렌더링
  ErrorCodeLogger          에러 코드의 로그 레벨로 기록
  ApiControllerAdvice      모든 예외 → ProblemDetail. CoreException 은 코드대로, Spring MVC 표준 예외는 fromStatus() 코드로,
                           검증 실패는 errors[{field, message}] 추가, 나머지는 INTERNAL_ERROR
```

핵심 규칙은 하나다. **어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러 코드가 결정한다.** 도메인, UseCase, 어댑터 어디서
던져도 같은 코드는 같은 상태·같은 제목으로 나간다.

### 에러 코드 목록

코드 문자열은 접두어로 컨텍스트를 구분한다. 접두어 뒤 숫자는 컨텍스트 안에서 순번이다.

| 접두어 | 영역 | 예 |
|---|---|---|
| `C` | 공통. 특정 컨텍스트에 속하지 않는 오류와 Spring MVC 표준 오류 | `C001` 400, `C002` 404, `C003` 405, `C004` 415, `C999` 500 |
| `S` | 샘플 컨텍스트 | `S001` 샘플 없음 (404), `S002` 이미 비활성 (409) |

`C999` 는 처리되지 않은 모든 예외의 기본값이다. 공통 코드는 `ErrorCode.fromStatus()` 가 참조하므로 삭제하거나 상태를 바꿀 때 함께 확인한다.

### 신규 에러 코드 추가하기

1. **컨텍스트 접두어 정하기.** 기존 접두어가 있으면 다음 번호를, 새 컨텍스트면 새 접두어를 쓴다. 접두어를 추가하면 `ErrorCode` 의
   클래스 주석과 이 README 의 표에도 적는다.
2. **`ErrorCode` 에 항목 추가.** 컨텍스트별 구분 주석 아래에 넣는다.

   ```java
   // ----- 주문 컨텍스트 (O) -----
   ORDER_NOT_FOUND("O001", 404, "Order not found.", LogLevel.INFO),
   ORDER_ALREADY_PAID("O002", 409, "Order has already been paid.", LogLevel.WARN),
   ORDER_PAYMENT_FAILED("O003", 502, "Payment provider rejected the request.", LogLevel.ERROR);
   ```

   - **코드 문자열**: 한 번 배포된 코드는 바꾸지 않는다. 클라이언트가 분기에 사용한다.
   - **HTTP 상태**: 클라이언트가 고칠 수 있는 문제면 4xx, 서버 쪽 문제면 5xx.
   - **제목**: 에러 *종류* 를 설명하는 문장. 특정 건의 값은 `detail` 이나 `property()` 로.
   - **로그 레벨**: 정상 흐름에서 생길 수 있는 클라이언트 오류는 `INFO`, 운영자가 알아야 하면 `WARN`, 조사가 필요하면 `ERROR`.
3. **`ErrorCodeTest` 실행.** 코드 문자열 중복과 상태 범위를 검사한다.
4. **문서 갱신.** `core/core-api/src/docs/asciidoc/index.adoc` 의 에러 코드 표에 한 줄 추가한다. 응답 모양을 보여줄 필요가
   있으면 컨트롤러 REST Docs 테스트에 오류 케이스를 추가한다 (`SampleControllerTest.getSampleNotFound()` 참고).

### 사용하기

**규칙 위반은 애그리거트가 던진다.** 상태 전이 규칙은 도메인에 있으므로 예외도 거기서 나온다.

```java
// Sample (core-domain)
public void deactivate() {
    if (this.status == SampleStatus.INACTIVE) {
        throw new CoreException(ErrorCode.SAMPLE_ALREADY_INACTIVE).property("sampleId", id);
    }
    this.status = SampleStatus.INACTIVE;
}
```

**없는 리소스는 UseCase 가 던진다.** 조회 결과가 없는 것은 도메인 규칙이 아니라 흐름의 문제다.

```java
// SampleUseCase (core-api)
Sample sample = sampleRepository.findById(sampleId)
    .orElseThrow(() -> new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", sampleId));
```

**발생 건의 정보를 붙인다.** 구조화된 값은 `property()` 로, 사람이 읽을 설명은 `detail` 로.

```java
throw new CoreException(ErrorCode.ORDER_ALREADY_PAID, "paidAt=" + order.getPaidAt());
```

`property()` 로 넣은 값은 응답 JSON 최상위에 그대로 노출된다. 민감 정보나 내부 식별자는 넣지 않고, 이름이 ProblemDetail 표준 필드
(`type`, `title`, `status`, `detail`, `instance`)와 겹치지 않게 한다.

**외부 연동 실패는 어댑터가 감싼다.** `clients` 어댑터가 Feign 예외를 받아 에러 코드로 바꾼다. 원인 예외는 로그에 남기고 응답에는 노출하지 않는다.

**컨트롤러는 아무것도 하지 않는다.** `try/catch` 없이 UseCase 를 호출하고 DTO 를 반환한다. 예외는 `ApiControllerAdvice` 가 받는다.

**비동기 메서드도 같다.** `@Async` 안에서 던진 `CoreException` 은 `AsyncExceptionHandler` 가 같은 로그 레벨로 기록한다.

**클라이언트는 `status` 가 아니라 `code` 로 분기한다.** 같은 404 라도 `C002`(경로 없음)와 `S001`(샘플 없음)은 다른 상황이다.

## 새 컨텍스트 추가하기

`sample` 컨텍스트가 레퍼런스다. 사람은 문서보다 옆 패키지를 복사하므로, 레퍼런스가 규칙을 어기지 않게 유지하는 것이 가장 강한 강제 수단이다.

1. `core-domain` 에 `com.meteor.<context>.domain` 을 만들고 애그리거트와 `*Repository` 포트를 둔다. 순수 단위 테스트를 같이 쓴다.
2. `storage/db-core` 에 `com.meteor.<context>.storage` 를 만들고 `*Entity`(package-private), `*JpaRepository`(package-private),
   `*RepositoryAdapter` 를 둔다. 테이블 이름은 컨텍스트 접두어.
3. `core-api` 에 `com.meteor.<context>.application` 의 `*UseCase`, `*Command`, `*Result` 와 `com.meteor.<context>.api` 의
   컨트롤러·DTO 를 둔다.
4. 다른 컨텍스트와 협력이 필요하면 상대 컨텍스트의 `application` 에 `*Facade` 인터페이스를 두거나, `*Event` 를 발행하고
   `@TransactionalEventListener` 로 받는다. 직접 import 는 R-07 이 막는다.
5. `ErrorCode` 에 접두어를 추가한다.
6. `./gradlew test` 로 R 규칙을 통과하는지 확인한다.

## 의존성 버전 관리

모든 버전은 `gradle.properties` 에서 관리한다. 새 라이브러리를 추가할 때는 버전을 `gradle.properties` 에 적고 `build.gradle` 에서 참조한다.

## 런타임 프로필

| 프로필 | 용도 |
|---|---|
| `local` | 네트워크 없이 개발. H2 인메모리 DB, DDL auto create |
| `local-dev` | 로컬에서 dev 환경 자원(DB 등)에 접속 |
| `dev` | 개발 서버 배포 |
| `staging` | 스테이징 서버 배포 |
| `live` | 운영 서버 배포 |

로그 설정은 `support/logging/src/main/resources/logback/logback-{profile}.xml` 로 프로필마다 분리된다.

## 테스트 태스크와 태그

| 태스크 | 포함 태그 | 설명 |
|---|---|---|
| `test` | develop, restdocs 제외 | CI 에서 실행하는 기본 묶음. ArchUnit 규칙 포함 |
| `unitTest` | 태그 없음 | 외부 의존 없는 빠른 단위 테스트 (도메인, ArchUnit) |
| `contextTest` | `context` | Spring 컨텍스트를 올리는 통합 테스트 |
| `restDocsTest` | `restdocs` | REST Docs 스니펫 생성 |
| `developTest` | `develop` | CI 에서 돌리지 않는 개발용 테스트 |

```bash
./gradlew test                       # CI 기본 (ArchUnit 포함)
./gradlew :core:core-api:asciidoctor # REST Docs HTML 생성 (restDocsTest 선행)
./gradlew checkFormat                # 코드 포맷 검사
./gradlew format                     # 코드 포맷 적용
```

## 실행

```bash
./gradlew :core:core-api:bootRun
curl localhost:8080/health
curl -X POST localhost:8080/api/v1/samples -H 'Content-Type: application/json' -d '{"name":"meteor"}'
curl localhost:8080/api/v1/samples/1
curl -X POST localhost:8080/api/v1/samples/1/deactivate
```

## 권장 IDE 설정 (IntelliJ IDEA)

- `Build, Execution, Deployment > Build Tools > Gradle > Run tests using` 을 `IntelliJ IDEA` 로 설정하면 테스트 실행이 빠르다.
- 포맷터는 [Spring Java Format](https://github.com/spring-io/spring-javaformat#intellij-idea) 플러그인을 사용한다.

## 관련 문서

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 규칙의 단일 출처. R-xx 구조 규칙, S-xx 의미 규칙, 예외 절차, 측정 항목
- [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) 리뷰 에이전트 입력·프롬프트·출력 형식·골든 세트·차단 범위
