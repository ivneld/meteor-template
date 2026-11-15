# 새 기능 추가와 규칙 변경

## 새 기능 추가

`member`, `order`, `payment` 세 도메인이 레퍼런스다. 사람은 문서보다 옆 코드를 복사하므로, 레퍼런스가 규칙을 어기지 않게 유지하는 것이
가장 강한 강제 수단이다.

| 본보기 | 코드 |
|---|---|
| usecase 없이 api 가 도메인 서비스를 바로 부름 | `MemberController` → `MemberReader`, `MemberWriter` |
| 여러 애그리거트에 걸친 규칙 | `OrderLimitPolicy` (사실은 `OrderWriter` 가 꺼냄) |
| 여러 도메인 서비스를 한 트랜잭션으로 묶는 usecase | `OrderUseCase.place` |
| 외부 호출을 사이에 두고 트랜잭션을 나누는 usecase | `PaymentUseCase.pay` |

1. core-enum 의 `com.meteor.enums` 에 상태·종류 enum 을 둔다.
2. `com.meteor.domain` 에 애그리거트를 `@Entity` 로 두고 `BaseEntity` 를 상속한다. 기본 생성자는 `protected`, 생성은 정적 팩토리,
   setter 는 두지 않는다(R-15, R-20). 다른 애그리거트는 ID 로만 참조한다(R-14). 순수 단위 테스트를 같이 쓴다. 상태 전이나 정책이 없는
   기능이면 단순한 모델로 시작한다(S-02).
3. 값 객체는 `@Embeddable` record 로 두고, 애그리거트에서 `@Embedded` 와 `@AttributeOverride` 로 컬럼 이름을 정한다(R-05).
4. `com.meteor.domain` 에 `JpaRepository<애그리거트, Long>` 를 상속한 `*Repository` 인터페이스를 둔다. 조회 조건은 인자로 받는다(S-08).
   왕복 테스트(`*RepositoryIT`)로 매핑을 확인한다.
5. `com.meteor.domain` 에 `*Reader` 와 `*Writer` 를 둔다. 자기 `*Repository` 만 쓰고, 상태를 바꾸면 save 한다(R-04, R-16).
6. 흐름이 애그리거트 하나로 끝나면 `com.meteor.api` 의 컨트롤러가 도메인 서비스를 바로 부른다. 여러 도메인 서비스를 엮거나 외부 시스템을
   부르면 `com.meteor.usecase` 에 `*UseCase` 와 `*Command` 를 둔다.
7. 응답 DTO 는 getter 만 읽어 만들고 필드에는 도메인 타입을 두지 않는다(R-12).
8. `ErrorCode` 에 접두어를 추가한다([에러 처리](../error-handling.md)).
9. `./gradlew test` 로 R 규칙을 통과하는지 확인하고, 코드리뷰 요청 전에 `/arch-review` 를 돌린다.

## 규칙을 지킬 수 없을 때

규칙을 지킬 수 없는 상황은 있다. 그때는 우회하지 말고 기록한다.

1. ArchUnit 위반이면 `FreezingArchRule` 로 해당 위반만 동결한다. 동결 파일은 저장소에 커밋한다.
2. `docs/adr/` 에 ADR 한 장을 쓴다. 규칙 ID, 이유, 만료 조건(날짜 또는 상황), 되돌리는 방법.
3. 분기마다 동결 목록과 ADR 을 리뷰해 만료된 것을 되돌린다.

리뷰 에이전트의 지적이 틀렸다고 판단하면 사람이 결정하고, 그 사례를 골든 세트에 넣는다([리뷰 에이전트](../review-agent.md)).

## 규칙을 바꿀 때

규칙 자체를 바꿔야 한다면 한 PR 에서 다음을 함께 고친다. `RuleCatalogTest` 가 ID 가 어긋나면 실패한다.

- [규칙 인덱스](rules.md) 와 규칙을 설명하는 장
- `core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java` (R 규칙)
- `harness/rules/` 의 에이전트용 규칙 파일
- 새 구조 규칙이 직접 만든 조건을 쓰면 `ArchitectureRulesSelfTest` 에 fixture

없어진 번호는 다른 규칙에 다시 쓰지 않고 인덱스 아래 목록에 남긴다.

## 측정

분기 리뷰에서 보는 숫자.

- 동결된 ArchUnit 위반 수
- `TransactionTemplate` 으로 트랜잭션을 나눈 usecase 메서드 수와 각각의 중간 상태 처리(대사·재시도) 여부
- domain 패키지의 클래스 수 (한 패키지가 감당하기 어려워지는 시점을 본다)
- `*Writer` 별 public 메서드 수 (비대해지는 서비스를 본다)
- 리뷰 에이전트 골든 세트의 규칙별 재현율·정밀도
