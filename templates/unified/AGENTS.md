# AGENTS.md — meteor-template (통합형)

이 파일은 이 코드베이스에서 일하는 AI 에이전트의 진입점이다. 사람용 설명은 `docs/`, 에이전트용 상세는 `harness/` 에 있다.

Spring Boot(Java 21) 애플리케이션 템플릿의 **통합형** 이다. 애그리거트가 곧 JPA 엔티티(`@Entity`)이고 `*Repository` 는 Spring Data 인터페이스다. 매핑 계층이 없는 대신 ID 참조, 연관관계 금지, setter 금지, 명시적 save 같은 규율로 나중에 분리할 수 있는 상태를 지킨다. 코드는 `domain`, `usecase`, `api` 세 계층으로만 나눈다. 다른 방식은 분리형(templates/separated) 템플릿이며, 템플릿을 바꾸는 판단은 사람이 한다.

## 작업 순서

1. 세션에서 처음 코드를 바꾸기 전에 `harness/context/background.md`(왜 이 구조인가)와 `harness/context/structure.md`(무엇을 어디에 두나, 레퍼런스 코드)를 읽는다.
2. 작업에 맞는 플레이북을 읽는다.

   | 작업 | 플레이북 |
   |---|---|
   | 새 애그리거트(상태와 식별자가 있는 개념) | `harness/playbooks/new-aggregate.md` |
   | 애그리거트 하나로 끝나는 엔드포인트 | `harness/playbooks/new-endpoint.md` |
   | 여러 도메인 서비스를 엮는 흐름 | `harness/playbooks/new-use-case.md` |
   | 외부 호출 등으로 트랜잭션을 나누는 흐름 | `harness/playbooks/split-transaction.md` |
   | 규칙을 지킬 수 없어 보일 때, 규칙을 바꿀 때 | `harness/playbooks/rule-exception.md` |

3. 고치는 경로의 규칙 파일을 읽는다(아래 "경로별 규칙"). 규칙 파일은 규칙, 이유, 올바른/잘못된 코드, 고치는 법을 담는다.
4. 옆의 레퍼런스 코드와 같은 모양으로 쓴다. 문서와 레퍼런스 코드가 다르면 레퍼런스 코드가 틀린 것이니 사람에게 알린다.
5. 끝내기 전에 "완료 확인" 을 돌린다.

## 절대 하지 않는 것

- ArchUnit 규칙(`ArchitectureRules`), 허용 목록, 동결 파일을 고쳐 빌드를 통과시키지 않는다. 막히면 `harness/playbooks/rule-exception.md`.
- `*Service`, `*Manager`, `*Handler`, `*Helper` 같은 이름을 만들지 않는다(R-03).
- usecase·도메인 서비스에서 애그리거트 상태로 `if`/`switch` 하지 않는다. 애그리거트 메서드로 내린다(S-01).
- 컨트롤러에 `@Transactional`, 저장소 호출, 둘 이상의 서비스 호출을 두지 않는다(R-06, R-10, R-11).
- 트랜잭션 안에서 외부 시스템을 부르거나(R-18), 도메인 서비스 예외를 잡아 삼키지 않는다(S-07).
- 애그리거트 사이에 JPA 연관관계를 걸거나, `EntityManager` 를 쓰거나, 엔티티를 응답으로 그대로 내보내지 않는다(R-14, R-05, R-12).

## 규칙 인덱스

`구조` 규칙은 `./gradlew test` 의 ArchUnit 이 막는다. `의미` 규칙은 코드 구조로 판별할 수 없어 리뷰 에이전트(`/arch-review`)가 본다. 둘 다 쓸 때부터 지킨다.

| ID | 규칙 | 강제 | 상세 |
|---|---|---|---|
| R-01 | 도메인 모델은 JPA 매핑 외의 프레임워크를 모른다 | ArchUnit | [harness/rules/R-01-domain-model-is-pure.md](harness/rules/R-01-domain-model-is-pure.md) |
| R-02 | 의존은 바깥에서 안쪽으로만 흐른다 | ArchUnit | [harness/rules/R-02-dependency-direction.md](harness/rules/R-02-dependency-direction.md) |
| R-03 | 클래스 이름은 닫힌 어휘에서 고른다 | ArchUnit | [harness/rules/R-03-closed-naming.md](harness/rules/R-03-closed-naming.md) |
| R-04 | 도메인 서비스는 자기 애그리거트의 *Repository 만 쓴다 | ArchUnit | [harness/rules/R-04-domain-service-scope.md](harness/rules/R-04-domain-service-scope.md) |
| R-05 | JPA 엔티티는 domain 의 애그리거트이고 영속화는 *Repository 로만 한다 | ArchUnit | [harness/rules/R-05-persistence-model-placement.md](harness/rules/R-05-persistence-model-placement.md) |
| R-06 | @Transactional 은 usecase 와 *Reader / *Writer 에만 있다 | ArchUnit | [harness/rules/R-06-where-transactions-are-declared.md](harness/rules/R-06-where-transactions-are-declared.md) |
| R-07 | *Reader 는 readOnly, *Writer 는 쓰기 트랜잭션이고 기본 전파만 쓴다 | ArchUnit | [harness/rules/R-07-domain-service-transactions.md](harness/rules/R-07-domain-service-transactions.md) |
| R-08 | 쓰기를 엮는 usecase 메서드는 바깥 트랜잭션을 연다 | ArchUnit | [harness/rules/R-08-use-case-transaction-guard.md](harness/rules/R-08-use-case-transaction-guard.md) |
| R-09 | usecase 는 도메인 서비스를 거치고 다른 usecase 를 부르지 않는다 | ArchUnit | [harness/rules/R-09-use-cases-go-through-domain-services.md](harness/rules/R-09-use-cases-go-through-domain-services.md) |
| R-10 | api 는 usecase 와 도메인 서비스만 부르고 애그리거트는 읽기만 한다 | ArchUnit | [harness/rules/R-10-what-api-may-use.md](harness/rules/R-10-what-api-may-use.md) |
| R-11 | 엔드포인트 하나는 usecase 나 도메인 서비스 메서드 하나만 부른다 | ArchUnit | [harness/rules/R-11-one-call-per-endpoint.md](harness/rules/R-11-one-call-per-endpoint.md) |
| R-12 | API 요청·응답 객체는 도메인 타입을 필드로 갖지 않는다 | ArchUnit | [harness/rules/R-12-api-dtos-carry-no-domain-types.md](harness/rules/R-12-api-dtos-carry-no-domain-types.md) |
| R-13 | core-enum 의 enum 은 아무것도 의존하지 않는다 | ArchUnit | [harness/rules/R-13-enums-have-no-dependencies.md](harness/rules/R-13-enums-have-no-dependencies.md) |
| R-14 | 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다 | ArchUnit | [harness/rules/R-14-reference-aggregates-by-id.md](harness/rules/R-14-reference-aggregates-by-id.md) |
| R-15 | 도메인 모델은 public setter 를 갖지 않는다 | ArchUnit | [harness/rules/R-15-no-setters-in-domain-model.md](harness/rules/R-15-no-setters-in-domain-model.md) |
| R-16 | 애그리거트를 바꾼 *Writer 메서드는 save 를 부른다 | ArchUnit | [harness/rules/R-16-explicit-save.md](harness/rules/R-16-explicit-save.md) |
| R-17 | *Reader 는 쓰지 않는다 | ArchUnit | [harness/rules/R-17-readers-do-not-write.md](harness/rules/R-17-readers-do-not-write.md) |
| R-18 | @Transactional 이 적용된 usecase 메서드는 외부 시스템을 부르지 않는다 | ArchUnit | [harness/rules/R-18-no-external-calls-in-transactions.md](harness/rules/R-18-no-external-calls-in-transactions.md) |
| R-19 | api 는 값 객체를 만들고 읽기만 한다 | ArchUnit | [harness/rules/R-19-api-does-not-compute-with-value-objects.md](harness/rules/R-19-api-does-not-compute-with-value-objects.md) |
| R-20 | 애그리거트는 자기 정적 팩토리로만 만든다 | ArchUnit | [harness/rules/R-20-aggregates-created-by-factories.md](harness/rules/R-20-aggregates-created-by-factories.md) |
| S-01 | usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다 | 리뷰 WARN | [harness/rules/S-01-no-branching-on-domain-state.md](harness/rules/S-01-no-branching-on-domain-state.md) |
| S-02 | 규칙이 있는 애그리거트는 빈약하지 않다 | 리뷰 WARN | [harness/rules/S-02-aggregates-are-not-anemic.md](harness/rules/S-02-aggregates-are-not-anemic.md) |
| S-04 | 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다 | 리뷰 WARN | [harness/rules/S-04-cross-aggregate-rules-in-policy.md](harness/rules/S-04-cross-aggregate-rules-in-policy.md) |
| S-06 | 트랜잭션을 나눈 usecase 는 중간 상태와 재실행 안전성을 설계한다 | 리뷰 WARN | [harness/rules/S-06-split-transactions-are-designed.md](harness/rules/S-06-split-transactions-are-designed.md) |
| S-07 | 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다 | 리뷰 BLOCK | [harness/rules/S-07-do-not-swallow-inside-transactions.md](harness/rules/S-07-do-not-swallow-inside-transactions.md) |
| S-08 | 저장소 쿼리와 client 에 규칙을 넣지 않는다 | 리뷰 WARN | [harness/rules/S-08-adapters-only-translate.md](harness/rules/S-08-adapters-only-translate.md) |
| S-09 | JPA 가 도메인 설계를 정하게 두지 않는다 | 리뷰 WARN | [harness/rules/S-09-jpa-does-not-shape-the-model.md](harness/rules/S-09-jpa-does-not-shape-the-model.md) |

없는 번호: S-03 → R-19, S-05 → R-16, R-21 분리형 전용. 없앤 번호는 다시 쓰지 않는다.

## 경로별 규칙

| 경로 | 읽을 규칙 |
|---|---|
| `core/core-api/src/main/java/com/meteor/domain/**` | R-01, R-03, R-04, R-05, R-07, R-14, R-15, R-16, R-17, R-20, S-01, S-02, S-04, S-08, S-09 |
| `core/core-api/src/main/java/com/meteor/usecase/**` | R-03, R-06, R-08, R-09, R-18, S-01, S-04, S-06, S-07 |
| `core/core-api/src/main/java/com/meteor/api/**` | R-03, R-10, R-11, R-12, R-19 |
| `storage/db-core/src/main/java/**` | R-02, R-05 |
| `core/core-enum/src/main/java/**` | R-13 |
| `clients/*/src/main/java/**` | R-02, S-08 |
| `**/src/test/**` | 테스트 작성 규칙은 각 플레이북의 테스트 단계 |

## 완료 확인

```bash
./gradlew format          # 포맷 적용 (Spring Java Format)
./gradlew checkFormat test   # CI 와 같다. ArchUnit 포함
./gradlew restDocsTest    # api 를 바꿨으면
```

- 테스트가 실패하면 실패한 규칙 ID 의 규칙 파일을 읽고 코드를 고친다. 규칙을 고치지 않는다.
- 의미 규칙(S)은 빌드가 보지 못한다. 바꾼 코드를 S 규칙 파일의 "위반 신호" 로 직접 점검하거나, 사람이 요청하면 `/arch-review` 를 쓴다.
- 규칙, 규칙 문서, ArchUnit 을 바꿨으면 `RuleCatalogTest` 가 함께 통과해야 한다(아래 "하네스 유지").

## 하네스 지도

```
AGENTS.md                     이 파일. 진입점과 규칙 인덱스
CLAUDE.md                     Claude Code 용. AGENTS.md 를 불러온다
.claude/rules/*.md            Claude Code 가 경로를 건드릴 때 자동으로 읽는 경로별 요약
.claude/skills/arch-review/   Claude Code 에서 의미 규칙 리뷰를 수동으로 돌리는 절차
.pi/                          Pi 의 /arch-review 확장과 스킬 (pre-push 훅이 쓴다)
harness/
├── context/                    background.md(설계 배경) structure.md(설계된 구조, 레퍼런스 코드) errors.md(에러 처리)
├── rules/                      규칙 하나에 파일 하나. frontmatter(id, kind, archunit_fields, related, human_doc) + 본문
├── playbooks/                  작업 유형별 단계
└── review/                     리뷰 에이전트 프롬프트, 골든 세트, eval.sh
docs/                         사람용 문서. 같은 규칙을 장(章) 단위 설명으로 담는다
```

## 하네스 유지

규칙은 네 곳에 같은 ID 로 있다: ArchUnit 필드(`Rnn_` 접두어, 구조 규칙만), `harness/rules/<ID>-*.md`, 이 파일의 규칙 인덱스, `docs/architecture/rules.md`. `core/core-api/src/test/java/com/meteor/architecture/RuleCatalogTest.java` 가 네 곳의 ID 집합과 규칙 파일의 `archunit_fields`, `human_doc` 링크가 맞는지 검사한다. 규칙을 추가·변경·삭제할 때는 한 변경에서 네 곳을 함께 고친다.
