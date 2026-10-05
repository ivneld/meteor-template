# 아키텍처 규칙

이 문서는 템플릿이 강제하는 규칙의 단일 출처다. 규칙마다 ID 가 있고, ArchUnit 테스트(`core-api` 의 `ArchitectureRules`)와
리뷰 에이전트 체크리스트([REVIEW_AGENT.md](REVIEW_AGENT.md))가 같은 ID 를 쓴다. 규칙을 바꾸려면 이 문서를 먼저 고치고
테스트와 체크리스트를 따라 고친다.

- `R-xx` 구조 규칙. 결정적이며 ArchUnit 이 CI 에서 차단한다.
- `S-xx` 의미 규칙. 코드 구조만으로 판단할 수 없어 리뷰 에이전트와 사람이 본다.

## 계층과 모듈

```
core-shared     어휘. enum, VO, ErrorCode, CoreException. 외부 의존 0
core-domain     애그리거트, 도메인 서비스(*Policy 등), 리포지토리 포트. Spring/JPA 를 모름
core-api        application(UseCase, Command, Result) + api(Controller, Request/Response) + support(설정, 어드바이스)
storage/*       포트의 JPA 구현. 엔티티는 이 안에서만 존재
clients/*       외부 시스템 어댑터
support/*       logging, monitoring
```

```
        api ──► application ──► domain ◄── storage
         │           │            ▲          │
         └───────────┴────────────┴──────────┴──► core-shared
```

패키지는 컨텍스트가 계층보다 위에 온다. `com.meteor.<context>.{domain|application|api|storage|clients}`.
`com.meteor.shared` 와 `com.meteor.support` 는 컨텍스트가 아닌 공통 영역이다.

## 구조 규칙 (R)

| ID | 규칙 | 이유 |
|---|---|---|
| R-01 | `domain` 은 Spring, JPA, `application`, `api`, `storage`, `clients` 에 의존하지 않는다 | 규칙이 프레임워크와 분리되어야 순수 단위 테스트가 가능하고, 어느 실행 모듈에서든 재사용된다 |
| R-02 | `@Transactional` 은 `application` 에만 있다 (클래스·메서드 모두) | 트랜잭션 경계가 한 계층에만 있어야 "이 코드가 어느 트랜잭션 안인가"가 호출 스택 한 칸 위에서 결정된다 |
| R-03 | `api` 는 `domain`, `storage`, `clients` 에 의존하지 않는다 | 컨트롤러가 애그리거트 메서드를 부르면 트랜잭션 밖에서 상태가 바뀌고 저장이 누락된다. enum·VO 는 `core-shared` 에 있으므로 자연히 허용된다 |
| R-04 | `*UseCase` 는 다른 `*UseCase` 를 호출하지 않는다 | 트랜잭션 경계가 중첩되지 않게 한다. 공통 흐름은 도메인 서비스나 `*Facade` 로 |
| R-05 | `domain` 에 `*Service` 접미어 금지. `application` 의 클래스는 `*UseCase`, `*Command`, `*Result`, `*Facade`, `*Event` 중 하나 | "로직 있는 서비스"와 "트랜잭션만 묶는 서비스"가 공존하는 상황을 이름에서부터 막는다 |
| R-06 | `storage`, `clients` 어댑터는 `application`, `api` 에 의존하지 않는다 | 어댑터는 포트의 구현일 뿐이며 흐름을 알면 안 된다 |
| R-07 | 컨텍스트끼리 직접 의존하지 않는다. 허용 통로는 상대 컨텍스트의 `*Facade` 와 `*Event` 뿐 | 컨텍스트 경계가 코드에 있어야 분리 시점에 경계를 찾지 않아도 된다 |
| R-08 | JPA 엔티티는 `storage` 에만 있고 밖에서 참조하지 않는다. `domain` 의 `*Repository` 는 인터페이스다 | 영속 모델이 API 계약이나 도메인이 되는 것을 막는다. 변환은 어댑터 안에서 끝난다 |
| R-09 | `core-shared` 는 다른 모듈과 프레임워크에 의존하지 않는다 | 모든 계층이 참조하므로 의존이 생기면 전파된다. 나중에 서비스 간 공유 라이브러리가 된다 |

## 의미 규칙 (S)

ArchUnit 이 볼 수 없는 규칙. 리뷰 에이전트가 PR 마다 확인한다.

| ID | 규칙 | 신호 |
|---|---|---|
| S-01 | UseCase 는 도메인 상태로 분기하지 않는다 | UseCase 안의 `if (sample.getStatus() == ...)`. 애그리거트 메서드로 내린다 |
| S-02 | 애그리거트는 빈약하지 않다 | getter 만 있고 상태 변경이 밖에서 일어남. setter 존재 |
| S-03 | 컨트롤러는 VO 를 생성·변환만 하고 연산하지 않는다 | 컨트롤러에서 `money.multiply(...)` 같은 계산 |
| S-04 | 컨텍스트 간 참조는 ID 로만 한다 | 다른 컨텍스트의 애그리거트를 필드로 보유, JPA 연관관계 월경 |
| S-05 | 트랜잭션은 컨텍스트를 넘지 않는다 | 한 UseCase 가 두 컨텍스트의 포트를 주입받음. 후속 처리는 `AFTER_COMMIT` 이벤트로 |
| S-06 | 테이블은 컨텍스트가 소유한다 | 다른 컨텍스트 테이블의 FK, JOIN, 네이티브 쿼리 |
| S-07 | 이벤트 리스너는 커밋 이후에 동작한다 | `@TransactionalEventListener(phase = BEFORE_COMMIT)`, 동기적으로 상대 결과를 기다리는 리스너 |
| S-08 | `core-shared` 에는 값 연산만 있고 정책은 없다 | 비즈니스 기준값(무료 배송 금액 등)을 가진 VO 가 shared 에 있음 |
| S-09 | 어댑터는 변환만 하고 규칙을 갖지 않는다 | storage/clients 안의 비즈니스 분기 |
| S-10 | 저장은 명시적이다 | 도메인 객체를 바꾸고 `repository.save()` 를 호출하지 않는 UseCase |

## 예외 처리

규칙을 지킬 수 없는 상황은 있다. 그때는 우회하지 말고 기록한다.

1. ArchUnit 위반이면 `FreezingArchRule` 로 해당 위반만 동결한다. 동결 파일은 저장소에 커밋한다.
2. `docs/adr/` 에 ADR 한 장을 쓴다. 규칙 ID, 이유, 만료 조건(날짜 또는 상황), 되돌리는 방법.
3. 분기마다 동결 목록과 ADR 을 리뷰해 만료된 것을 되돌린다.

분리 가능성은 규칙이 아니라 예외의 개수로 결정된다. 예외 목록이 보여야 한다.

## 새 컨텍스트 추가

1. `com.meteor.<context>` 아래 `domain`, `application`, `api` 패키지를 만든다. storage 가 필요하면 `storage/db-core` 에 `com.meteor.<context>.storage`.
2. 다른 컨텍스트와 협력이 필요하면 상대 컨텍스트의 `application` 에 `*Facade` 인터페이스를 두거나 `*Event` 를 발행한다.
3. `ErrorCode` 에 접두어를 추가한다.
4. `./gradlew test` 로 R 규칙 통과를 확인한다. `sample` 컨텍스트가 레퍼런스다.

## 측정

분기 리뷰에서 보는 세 숫자.

- 동결된 ArchUnit 위반 수
- 컨텍스트 간 협력 중 이벤트 비율 (동기 Facade 호출 대비)
- S-06 위반(월경 쿼리) 수
