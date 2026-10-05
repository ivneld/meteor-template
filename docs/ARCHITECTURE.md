# 아키텍처 규칙

이 문서는 템플릿이 강제하는 규칙의 단일 출처다. 규칙마다 ID 가 있고, ArchUnit 테스트(`core-api` 의 `ArchitectureRules`)와
리뷰 에이전트 체크리스트([REVIEW_AGENT.md](REVIEW_AGENT.md))가 같은 ID 를 쓴다. 에러 코드는 [ERROR_HANDLING.md](ERROR_HANDLING.md), 구조를 키우는 시점과 순서는 [SCALING.md](SCALING.md) 에 있다. 규칙을 바꾸려면 이 문서를 먼저 고치고
테스트와 체크리스트를 따라 고친다.

- `R-xx` 구조 규칙. 결정적이며 ArchUnit 이 CI 에서 차단한다.
- `S-xx` 의미 규칙. 코드 구조만으로 판단할 수 없어 리뷰 에이전트와 사람이 본다.

## 계층과 모듈

```
core-shared     값 타입. record(VO) 와 enum 만. 외부 의존 0
core-domain     애그리거트, 도메인 서비스(*Policy 등), 에러 어휘(ErrorCode, CoreException). Spring/JPA 를 모름
core-api        application(UseCase, Command, Result) + api(Controller, Request/Response) + support(설정, 어드바이스)
storage/*       Repository 인터페이스와 JPA 구현. 엔티티는 이 안에서만 존재
clients/*       외부 시스템 어댑터
support/*       logging, monitoring
```

```
        api ──► application ──► domain ◄── storage / clients
         │           │            ▲              │
         └───────────┴────────────┴──────────────┴──► core-shared
```

### 패키지 규약

컨텍스트가 계층보다 위에 온다.

```
com.meteor.<context>.domain        애그리거트, 도메인 서비스(*Policy)                            (core-domain)
com.meteor.<context>.application   *UseCase, *Command, *Result, *Facade, *Event, *Listener    (core-api)
com.meteor.<context>.api           *Controller, request/*, response/*                          (core-api)
com.meteor.<context>.storage       *Repository(public), *Entity, *JpaRepository, *RepositoryAdapter (storage/db-core)
com.meteor.<context>.clients       외부 연동 어댑터                                             (clients/*)
com.meteor.shared                  record VO, enum                                              (core-shared)
com.meteor.support                 컨텍스트에 속하지 않는 공통 영역. 모듈을 가로지른다
  support.error                      ErrorCode, CoreException, LogLevel                        (core-domain)
  support.web / support.async        어드바이스, ProblemDetails, 설정                           (core-api)
  support.storage                    DataSource/JPA 설정, BaseEntity                            (storage/db-core)
  support.client                     컨텍스트에 속하지 않는 외부 클라이언트                     (clients/*)
```

### 계층별 책임

| 계층 | 하는 일 | 하지 않는 것 |
|---|---|---|
| `api` | HTTP ↔ Command/Result 변환. 엔드포인트 하나는 UseCase 메서드 하나 호출 | `@Transactional`, 도메인·저장소 접근, VO 연산 |
| `application` | 유스케이스 흐름. 꺼내고, 도메인 메서드를 부르고, 저장한다. `@Transactional` 의 유일한 자리 | 도메인 상태로 분기, 다른 UseCase 호출 |
| `domain` | 모든 규칙과 상태 전이. 식별자를 가진 가변 객체(애그리거트). 값 타입은 `core-shared` 의 record 를 쓴다 | Spring, JPA, 리포지토리, `*Service` 접미어 |
| `storage` / `clients` | Repository 인터페이스를 소유하고 구현한다. 엔티티 ↔ 도메인 변환 | 비즈니스 규칙, 트랜잭션 경계 |

### 영속화 결정

JPA 를 쓰되 **도메인 모델로 쓰지 않고 영속 엔진으로만 쓴다.** JPA 엔티티는 `storage` 안에 갇히고, 애그리거트는 순수 자바로 `core-domain` 에 있으며,
둘 사이 변환은 `*RepositoryAdapter` 가 한다. 그 결과 JPA 의 이점 중 일부는 포기하고 일부는 유지한다.

| 포기하는 것 (애그리거트 중심 설계가 원래 피하는 것) | 유지하는 것 (영속 모델에 붙는 인프라 기능) |
|---|---|
| 더티 체킹. 저장은 `save()` 로 명시한다 (S-09) | `@TenantId` 로 모든 쿼리에 테넌트 조건 자동 적용 |
| 지연 로딩과 연관관계 탐색. 애그리거트는 통째로 로딩한다 | `@Version` 낙관적 락, Envers 감사 이력, 2차 캐시 |
| cascade. 저장은 애그리거트 단위로 끝난다 | 스키마 매핑과 `validate`, 배치 insert, 방언 추상화 |

멀티테넌트, 감사 이력, 낙관적 락 중 하나라도 필요하면 이 구조를 유지한다. 그런 요구가 없는 작은 서비스는 변환 코드를 없애 주는
Spring Data JDBC 로 `storage` 모듈만 교체해도 된다. JPA 어노테이션을 애그리거트에 직접 붙이는 방식은 택하지 않는다. 포기한 쪽의
편의를 되찾는 대가로 경계를 어기기 쉬운 길(`@ManyToOne` 월경, 암묵적 저장)이 함께 돌아오기 때문이다.

`@Version` 을 쓸 때 애그리거트가 트랜잭션을 넘어 전달된다면 `restore()` 에 version 을 포함시켜 어댑터가 복원 시 넘기도록 한다.
같은 트랜잭션 안에서 읽고 저장하면 1차 캐시의 엔티티가 버전을 갖고 있으므로 추가 작업이 없다.

## 구조 규칙 (R)

| ID | 규칙 | 이유 |
|---|---|---|
| R-01 | `domain` 은 Spring, JPA, `application`, `api`, `storage`, `clients` 에 의존하지 않는다 | 규칙이 프레임워크와 분리되어야 순수 단위 테스트가 가능하고, 어느 실행 모듈에서든 재사용된다 |
| R-02 | `@Transactional` 은 `application` 에만 있다 (클래스·메서드 모두) | 트랜잭션 경계가 한 계층에만 있어야 "이 코드가 어느 트랜잭션 안인가"가 호출 스택 한 칸 위에서 결정된다 |
| R-03 | `api` 는 `domain`, `storage`, `clients` 에 의존하지 않는다 | 컨트롤러가 애그리거트 메서드를 부르면 트랜잭션 밖에서 상태가 바뀌고 저장이 누락된다. enum·VO 는 `core-shared`, 에러 어휘는 `support.error` 에 있으므로 자연히 허용된다 |
| R-04 | `*UseCase` 는 다른 `*UseCase` 를 호출하지 않는다 | 트랜잭션 경계가 중첩되지 않게 한다. 공통 흐름은 도메인 서비스나 `*Facade` 로 |
| R-05 | `domain` 에 `*Service` 접미어 금지. `application` 의 클래스는 `*UseCase`, `*Command`, `*Result`, `*Facade`, `*Event`, `*Listener` 중 하나 | "로직 있는 서비스"와 "트랜잭션만 묶는 서비스"가 공존하는 상황을 이름에서부터 막는다 |
| R-06 | `storage`, `clients` 어댑터는 `application`, `api` 에 의존하지 않는다 | 어댑터는 저장·연동의 구현일 뿐이며 흐름을 알면 안 된다 |
| R-07 | 컨텍스트끼리 직접 의존하지 않는다. 허용 통로는 상대 컨텍스트의 `*Facade` 와 `*Event` 뿐 | 컨텍스트 경계가 코드에 있어야 분리 시점에 경계를 찾지 않아도 된다 |
| R-08 | JPA 엔티티는 `storage` 에만 있고 밖에서 참조하지 않는다 | 영속 모델이 API 계약이나 도메인이 되는 것을 막는다. 변환은 어댑터 안에서 끝나고, application 은 public `*Repository` 인터페이스만 본다 |
| R-09 | `core-shared` 에는 record 와 enum 만 있고, 다른 모듈과 프레임워크에 의존하지 않는다 | "값이면 shared, 가변 객체면 domain" 하나로 판단이 끝난다. 모든 계층이 참조하므로 의존이 생기면 전파된다 |

## 의미 규칙 (S)

ArchUnit 이 볼 수 없는 규칙. 리뷰 에이전트가 PR 마다 확인한다.

| ID | 규칙 | 신호 |
|---|---|---|
| S-01 | UseCase 는 도메인 상태로 분기하지 않는다 | UseCase 안의 `if (order.getStatus() == ...)`. 애그리거트 메서드로 내린다 |
| S-02 | 애그리거트는 빈약하지 않다 | getter 만 있고 상태 변경이 밖에서 일어남. setter 존재 |
| S-03 | 컨트롤러는 VO 를 생성·변환만 하고 연산하지 않는다 | 컨트롤러에서 `money.multiply(...)` 같은 계산 |
| S-04 | 컨텍스트 간 참조는 ID 로만 한다 | 다른 컨텍스트의 애그리거트를 필드로 보유, JPA 연관관계 월경 |
| S-05 | 트랜잭션은 컨텍스트를 넘지 않는다 | 한 UseCase 가 두 컨텍스트의 Repository 를 주입받음. 후속 처리는 `AFTER_COMMIT` 이벤트로 |
| S-06 | 테이블은 컨텍스트가 소유한다 | 다른 컨텍스트 테이블의 FK, JOIN, 네이티브 쿼리 |
| S-07 | 이벤트 리스너는 커밋 이후에 동작한다 | `@TransactionalEventListener(phase = BEFORE_COMMIT)`, 동기적으로 상대 결과를 기다리는 리스너 |
| S-08 | 어댑터는 변환만 하고 규칙을 갖지 않는다 | storage/clients 안의 비즈니스 분기 |
| S-09 | 저장은 명시적이다 | 도메인 객체를 바꾸고 `repository.save()` 를 호출하지 않는 UseCase |

## 예외 처리

규칙을 지킬 수 없는 상황은 있다. 그때는 우회하지 말고 기록한다.

1. ArchUnit 위반이면 `FreezingArchRule` 로 해당 위반만 동결한다. 동결 파일은 저장소에 커밋한다.
2. `docs/adr/` 에 ADR 한 장을 쓴다. 규칙 ID, 이유, 만료 조건(날짜 또는 상황), 되돌리는 방법.
3. 분기마다 동결 목록과 ADR 을 리뷰해 만료된 것을 되돌린다.

분리 가능성은 규칙이 아니라 예외의 개수로 결정된다. 예외 목록이 보여야 한다.

### 값 타입의 검증 실패

`core-shared` 는 외부 의존이 없으므로 VO 생성자의 검증 실패는 `IllegalArgumentException` 으로 던진다.
`ApiControllerAdvice` 가 이를 `C001` 로 바꾼다. 도메인 규칙 위반은 `CoreException` 으로 던진다. 둘의 구분은 "값 자체가 성립하지 않는가"
(`Money(-1)`) 와 "값은 맞지만 지금 상태에서 허용되지 않는가" (`order.cancel()` on PAID) 다.

## 컨텍스트 식별

컨텍스트(`com.meteor.<context>`)는 바운디드 컨텍스트 하나를 코드로 옮긴 단위다. 경계를 어디에 그을지는 도구가 정해주지 않는다.
아래 테스트로 긋고, 아래 신호로 잘못 그었는지 확인한다.

### 경계를 찾는 테스트

| 테스트 | 질문 | 답이 "그렇다"면 |
|---|---|---|
| 언어 | 같은 단어가 다른 뜻으로 쓰이는가 (카탈로그의 상품 vs 주문의 상품) | 다른 컨텍스트 |
| 트랜잭션 | 이 둘이 같은 순간에 함께 바뀌지 않아도 비즈니스가 괜찮은가 | 다른 컨텍스트 후보 |
| 변경 이유 | 바뀌는 이유(이해관계자, 규제, 외부 연동)가 다른가 | 다른 컨텍스트 |
| 생명주기 | 자기만의 상태 기계를 갖고 다른 것과 독립적으로 시작·종료하는가 | 다른 컨텍스트 후보 |
| 참조 | 상대의 ID 만 있으면 충분한가 | 경계가 맞다 |

언어 테스트와 트랜잭션 테스트가 분명하게 "다르다"고 답하는 곳은 처음부터 긋는다. 애매한 곳은 하나로 두었다가 아래 신호가
나타날 때 나눈다. 모놀리스 안에서 두 컨텍스트를 합치는 것은 패키지 이동으로 끝나지만, 뭉친 것을 나중에 쪼개는 것은 얽힌 참조를
풀어야 해서 비싸다.

이벤트 스토밍으로 비즈니스 이벤트를 시간순으로 나열하면 흐름이 꺾이는 축(주문 확정, 결제 완료, 배송 시작)이 보이고, 축 사이에
묶이는 커맨드와 애그리거트가 컨텍스트 하나다. 축이 되는 이벤트는 그대로 컨텍스트 간 `*Event` 가 된다.

### 잘못 나누었다는 신호

| 신호 | 의미 | 조치 |
|---|---|---|
| 두 컨텍스트 사이에 Facade 호출이 **양방향**으로 오간다 | 둘은 하나의 모델을 둘로 찢은 것이다 | 합친다 |
| 한 유스케이스가 상대 Facade 를 **여러 번** 호출하며 흐름을 완성한다 (chatty) | 흐름의 소유자가 잘못됐거나 경계가 흐름 한가운데를 지난다 | 흐름이 속한 쪽으로 UseCase 를 옮기거나 합친다 |
| 이벤트가 상대 애그리거트의 **필드 전부**를 실어 나른다 | 수신 측이 상대 모델을 그대로 필요로 한다. 언어가 같다는 뜻이다 | 합치거나, 수신 측이 정말 필요한 값만 담은 이벤트로 줄인다 |
| 상대의 ID 가 아니라 **내부 필드**를 계속 읽는다 (S-04 반복) | 참조 테스트 실패. 데이터 소유자가 잘못됐다 | 그 데이터를 읽는 쪽으로 소유권을 옮기거나 합친다 |
| 한 UseCase 가 두 컨텍스트의 Repository 를 주입받는다 (S-05 반복) | 트랜잭션 테스트 실패. 함께 바뀌어야 하는 것을 갈랐다 | 합친다. 정말 최종 일관성으로 충분하면 이벤트로 바꾼다 |
| `shared` 나 `common` 성격의 컨텍스트가 **자꾸 커진다** | 소속을 정하지 못한 모델이 쌓이고 있다. 보통 경계가 모호한 것이다 | 각 항목을 언어 테스트로 다시 분류한다. `core-shared` 는 값 연산만 남긴다 |
| 같은 이름의 클래스가 두 컨텍스트에 **동일한 필드**로 존재한다 | 언어가 같은데 억지로 나눴다 | 합친다. 필드가 다르면 정상이다 |
| 한 컨텍스트가 다른 컨텍스트 **없이는 테스트할 수 없다** | 독립적인 모델이 아니다 | 의존 방향을 한쪽으로 정리하거나 합친다 |
| 상대 컨텍스트의 **상태 변화를 폴링**하거나 조회로 흐름을 이어간다 | 이벤트가 있어야 할 자리에 없다 | 축이 되는 이벤트를 정의하고 발행한다 |
| 한 변경 요청이 **항상 두 컨텍스트를 동시에** 건드린다 | 변경 이유 테스트 실패 | 합친다 |
| 한 컨텍스트가 **너무 커서** 한 팀이 모델 전체를 설명하지 못한다 | 언어가 둘 이상 섞여 있다 | 언어 테스트로 다시 나눈다 |

신호는 대부분 "합친다"로 끝난다. 처음에 너무 잘게 나눈 비용이 너무 크게 나눈 비용보다 흔하기 때문이다. 반대로 마지막 두 신호는
"나눈다"로 끝나며, 이쪽은 ArchUnit 이 잡지 못하므로 분기 리뷰에서 사람이 본다.

신호가 보이면 경계를 바꾸는 것을 미루지 않는다. 경계가 틀린 채로 쌓인 코드는 서비스 분리 시점에 전부 다시 풀어야 한다.

## 새 컨텍스트 추가

`member`, `order`, `payment`, `shipping` 네 컨텍스트가 레퍼런스다. 동기 질의는 `order → MemberFacade`, `payment → OrderFacade`,
커밋 후 통지는 `PaymentCompletedEvent → OrderEventListener`, `OrderPaidEvent → ShippingEventListener` 를 본보기로 삼는다.
사람은 문서보다 옆 패키지를 복사하므로, 레퍼런스가 규칙을 어기지 않게 유지하는 것이 가장 강한 강제 수단이다.

경계를 어디에 그을지는 [컨텍스트 식별](#컨텍스트-식별) 절의 테스트로 정하고, 같은 절의 "잘못 나누었다는 신호"로 분기마다 점검한다.

1. `core-domain` 에 `com.meteor.<context>.domain` 을 만들고 애그리거트를 둔다. 순수 단위 테스트를 같이 쓴다. 값 타입이 필요하면
   `core-shared` 에 record 로 둔다.
2. `storage/db-core` 에 `com.meteor.<context>.storage` 를 만들고 `*Repository`(public 인터페이스, 도메인 객체를 주고받음),
   `*Entity`(package-private), `*JpaRepository`(package-private), `*RepositoryAdapter`(package-private) 를 둔다. 테이블 이름은 컨텍스트 접두어.
3. `core-api` 에 `com.meteor.<context>.application` 의 `*UseCase`, `*Command`, `*Result` 와 `com.meteor.<context>.api` 의
   컨트롤러·DTO 를 둔다.
4. 다른 컨텍스트와 협력이 필요하면 두 통로 중 하나를 쓴다. 직접 import 는 R-07 이 막는다.
   - 즉시 답이 필요한 질의: 상대 컨텍스트의 `application` 에 `*Facade` 를 둔다. 반환 타입은 `core-shared` 의 값이나 원시 타입으로 제한해
     상대 모델이 새지 않게 한다.
   - 후속 처리: `*Event` 를 발행하고 받는 쪽 `application` 에 `*Listener` 를 둔다. 리스너는 `@TransactionalEventListener`(커밋 후)로 받고,
     호출하는 UseCase 메서드는 `REQUIRES_NEW` 로 새 트랜잭션을 연다. 발행 측 트랜잭션은 이미 끝났기 때문이다.
5. `ErrorCode` 에 접두어를 추가한다.
6. `./gradlew test` 로 R 규칙을 통과하는지 확인한다.

## 측정

분기 리뷰에서 보는 숫자.

- 동결된 ArchUnit 위반 수
- 컨텍스트 간 협력 중 이벤트 비율 (동기 Facade 호출 대비)
- S-06 위반(월경 쿼리) 수
- 컨텍스트 쌍별 Facade 호출 방향 (양방향인 쌍의 수)
