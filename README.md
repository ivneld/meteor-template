# meteor-template

Spring Boot(Java) 서비스를 팀이 함께 개발하기 위한 템플릿.

목표는 두 가지다. **도메인 규칙은 애그리거트 한 곳에 모으고**, **컨텍스트 경계는 지금의 유지보수와 새 애플리케이션과의 연동을 위해
지킨다.** 서비스 분리를 미리 대비하지 않는다. 경계를 지킨 결과로 분리 가능성이 따라올 뿐이다. 판단 기준은
[설계 원칙](docs/ARCHITECTURE.md#설계-원칙)에 있다.

- 배포 단위는 하나, 바운디드 컨텍스트는 여러 개. 컨텍스트(`member`, `order`, `payment`, `shipping` ...) 하나는 `core-api` 안의 폴더 하나다.
- 컨텍스트 안은 `domain / application / api / storage` 네 패키지. 도메인 규칙은 `domain` 에만, 트랜잭션 경계는 `application` 에만 있다.
  애그리거트와 JPA 엔티티는 분리되어 있고 어댑터가 변환한다.
- 컨텍스트끼리는 ID 와 `*Facade`(판단 결과 반환), `*Event` 로만 협력한다. 이 경계들은 ArchUnit 테스트와 리뷰 에이전트가 강제한다.

## 모듈 구성

```
meteor-template
├── core
│   └── core-api        유일한 실행 모듈. 컨텍스트마다 domain / application / api / storage 패키지
│                       + shared(컨텍스트 간 계약 값) + support(에러 어휘, 웹 설정, 어드바이스)
├── storage
│   └── db-core         저장소 인프라. DataSource, JPA 설정, BaseEntity
├── clients
│   └── client-example  OpenFeign 기반 외부 연동 어댑터 예시
├── support
│   ├── logging         프로필별 logback, OpenTelemetry, Sentry
│   └── monitoring      Actuator + Prometheus
├── tests
│   └── api-docs        Spring REST Docs 테스트 베이스
└── docs
    ├── ARCHITECTURE.md   규칙, 계층, 컨텍스트 식별
    ├── ERROR_HANDLING.md 에러 코드와 ProblemDetail
    ├── SCALING.md        구조를 키우는 시점과 순서
    └── REVIEW_AGENT.md   리뷰 에이전트 하네스
```

### 컨텍스트 하나의 모양

```
core-api/src/main/java/com/meteor/order
├── domain        Order(애그리거트), OrderStatus, OrderLimitPolicy        ← 규칙은 여기에만
├── application   OrderUseCase, OrderFacade, OrderPaidEvent, OrderEventListener
├── api           OrderController, request/, response/
└── storage       OrderRepository(public 클래스, 엔티티 ↔ 애그리거트 변환)
                  OrderEntity, OrderJpaRepository (package-private)
```

```
        api ──► application ──► domain ◄── storage
         │           │            │           │
         └───────────┴────────────┴───────────┴──► shared, support.error
```

`domain` 은 Spring 과 JPA 를 모른다. `storage` 의 `*Repository` 는 Spring Data 를 감싸 도메인 객체만 주고받으므로 도메인에 의존한다.
영속화 기술은 JPA 로 고정이라 `*Repository` 위에 별도 인터페이스를 두지 않는다.
계층이 한 모듈에 있으므로 이 방향은 Gradle 이 아니라 ArchUnit(R-01 ~ R-10)이 강제한다. 같은 저장소에 배치·어드민 같은 두 번째 실행
모듈이 생기면 그때 `domain` 패키지를 모듈로 추출한다([SCALING.md](docs/SCALING.md)).

## 레퍼런스 도메인

구조를 보여주기 위한 최소한의 이커머스 흐름. 네 컨텍스트가 `*Facade`(판단을 돌려주는 동기 질의)와 `*Event` 로만 협력한다.
이벤트는 일관성 요구에 따라 두 가지로 받는다.

```
 member ◄──MemberFacade.ensureActive──── order ◄──OrderFacade.payableAmount──── payment
                                           │ ▲                                     │
                                           │ └── PaymentCompletedEvent ◄───────────┘
                                           │     (같은 트랜잭션: 결제와 주문 PAID 는 함께 커밋·롤백)
                                           │
                                           └──── OrderPaidEvent ────► shipping
                                                 (커밋 이후: 배송 준비는 확정 뒤의 부가 처리)
```

| 컨텍스트 | 애그리거트 | 규칙 | 다른 컨텍스트와의 접점 |
|---|---|---|---|
| `member` | `Member` | 탈퇴는 한 번만, 활성 회원만 다른 컨텍스트의 행위 주체가 됨 | `MemberFacade.ensureActive()` 를 공개 |
| `order` | `Order` | 수량 1 이상, CREATED 일 때만 결제·취소 가능. 회원당 결제 대기 주문은 3건까지(`OrderLimitPolicy`) | 회원을 `MemberFacade` 로 확인, `OrderFacade.payableAmount()` 공개, 결제 완료 이벤트를 결제 트랜잭션 안에서 받아 PAID, `OrderPaidEvent` 발행 |
| `payment` | `Payment` | 승인 금액은 0 보다 커야 함 | 금액을 `OrderFacade` 에 묻고 `PaymentCompletedEvent` 발행 |
| `shipping` | `Shipping` | READY → SHIPPED → DELIVERED | `OrderPaidEvent` 를 커밋 이후에 받아 배송 생성 |

값 타입은 소유자가 정한다. 컨텍스트 사이 계약에 실리는 `Money`, `Address` 만 `shared` 에 있고, `Email`, `PaymentMethod`, 상태 enum 은
각 컨텍스트의 `domain` 에 있다. 각 컨텍스트는 다른 컨텍스트를 ID(`memberId`, `orderId`)로만 안다.
전체 흐름은 `OrderFlowTest`, 결제와 주문의 원자성은 `PaymentOrderAtomicityTest` 가 검증한다.

## 핵심 규칙

- `domain` 은 Spring, JPA, 다른 계층을 모른다. 여러 애그리거트에 걸친 규칙은 `*Policy` 에 둔다.
- `@Transactional` 은 `application` 에만 있다.
- 컨텍스트끼리 직접 의존하지 않는다. `*Facade` 와 `*Event` 로만. Facade 는 모델이 아니라 판단 결과를 돌려준다.
- 함께 바뀌어야 하는 컨텍스트 간 처리는 같은 트랜잭션으로, 확정 뒤의 부가 처리만 커밋 이후로 받는다.

전체 규칙과 이유는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). 구조 규칙은 `./gradlew test` 의 `ArchitectureRules` 가 검사한다.

## 실행

```bash
./gradlew :core:core-api:bootRun
H='Content-Type: application/json'
curl -X POST localhost:8080/api/v1/members  -H "$H" -d '{"email":"kim@example.com","name":"kim"}'
curl -X POST localhost:8080/api/v1/orders   -H "$H" -d '{"memberId":1,"productName":"keyboard","quantity":2,"unitPrice":50000,"shippingAddress":{"city":"Seoul","street":"Teheran-ro 1","zipCode":"06000"}}'
curl -X POST localhost:8080/api/v1/payments -H "$H" -d '{"orderId":1,"method":"CARD"}'
curl localhost:8080/api/v1/orders/1                 # status: PAID (이벤트로 전이)
curl 'localhost:8080/api/v1/shippings?orderId=1'    # status: READY (이벤트로 생성)
curl -X POST localhost:8080/api/v1/shippings/1/ship
```

```bash
./gradlew test                       # CI 기본. ArchUnit 포함
./gradlew :core:core-api:asciidoctor # REST Docs HTML 생성 (restDocsTest 선행)
./gradlew checkFormat                # 코드 포맷 검사
./gradlew format                     # 코드 포맷 적용
```

## 코드리뷰 요청 전

```bash
npm i -g @earendil-works/pi-coding-agent   # 최초 1회
cp .env.example .env                        # 모델(ARCH_REVIEW_MODEL)과 토큰(ANTHROPIC_API_KEY 등) 입력. .env 는 커밋되지 않는다
pi                                          # 저장소 루트에서 실행해 프로젝트를 신뢰(/trust)한 뒤, 프롬프트에 /arch-review
pi --approve -p --no-session "/arch-review" < /dev/null   # 비대화형. 종료 코드 0 PASS / 1 BLOCK / 2 WARN
```

`/arch-review` 는 저장소에 포함된 Pi 확장(`.pi/extensions`)이다. ArchUnit 이 구조 규칙을, 리뷰 에이전트가 의미 규칙을 본다.
BLOCK 이면 고치거나 ADR 로 예외를 남긴 뒤 리뷰를 요청한다. 푸시마다 자동으로 돌리려면 `git config core.hooksPath .githooks`.
설정과 사용법은 [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md).

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

## 권장 IDE 설정 (IntelliJ IDEA)

- `Build, Execution, Deployment > Build Tools > Gradle > Run tests using` 을 `IntelliJ IDEA` 로 설정하면 테스트 실행이 빠르다.
- 포맷터는 [Spring Java Format](https://github.com/spring-io/spring-javaformat#intellij-idea) 플러그인을 사용한다.

## 관련 문서

| 문서 | 언제 보는가 |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 설계 원칙, 코드를 어디에 둘지, 규칙을 어겨야 할 때, 새 컨텍스트를 만들 때, 경계가 맞는지 점검할 때 |
| [docs/ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | 에러 코드를 추가하거나 예외를 던질 때, 오류 응답 형식을 알아야 할 때 |
| [docs/SCALING.md](docs/SCALING.md) | 트래픽·조직이 커져 구조를 바꿔야 하는지, 새 기능을 별도 애플리케이션으로 만들지, 다른 앱과 연동할 때 |
| [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) | 코드리뷰 요청 전 `/arch-review` 를 돌릴 때, 결과를 해석할 때, 프롬프트를 바꿀 때 |
