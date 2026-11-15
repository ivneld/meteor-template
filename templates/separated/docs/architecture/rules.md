# 규칙 인덱스

이 템플릿이 강제하는 규칙 전체 목록이다. 규칙의 이유와 예시는 ID 링크를 따라 각 장에서 읽는다.

- `R-xx` 구조 규칙. 결정적이며 ArchUnit(`ArchitectureRules`)이 `./gradlew test` 에서 차단한다. 테스트 필드 이름이 `Rxx_` 로 시작한다.
- `S-xx` 의미 규칙. 코드 구조만으로 판단할 수 없어 리뷰 에이전트(`/arch-review`)와 사람이 본다. BLOCK 은 푸시를 막고 WARN 은 사유를 남긴다.

규칙을 바꾸려면 이 표, 해당 장, `ArchitectureRules`, 에이전트용 규칙 파일(`harness/rules/`)을 같은 PR 에서 고친다. `RuleCatalogTest` 가
네 곳의 ID 가 어긋나면 실패한다. 없어진 번호는 다른 규칙에 다시 쓰지 않는다.

| ID | 규칙 | 강제 | 이유 |
|---|---|---|---|
| [R-01](domain-model.md#R-01) | 도메인 모델은 Spring, JPA, storage, client, 상위 계층, 도메인 빈을 모른다 | ArchUnit | 목 없이 단위 테스트하고, core-domain 추출을 파일 이동으로 끝낸다 |
| [R-02](structure.md#R-02) | usecase 는 api 를, storage 와 client 는 domain·usecase·api 를 모른다 | ArchUnit | 의존은 바깥에서 안쪽으로만 흐른다 |
| [R-03](structure.md#R-03) | 이름은 닫힌 어휘(`*Reader`, `*Writer`, `*Repository`, `*UseCase`, `*Command`, `*Result`, `*Controller`, `*Request`, `*Response`)에서 고른다 | ArchUnit | 이름만으로 역할이 드러나야 규칙·흐름·트랜잭션이 섞이지 않는다 |
| [R-04](domain-services.md#R-04) | `*Reader` / `*Writer` 는 같은 이름의 `*Repository` 만 쓴다 | ArchUnit | 도메인 서비스가 다른 애그리거트로 넘어가면 비대해진다 |
| [R-05](persistence.md#R-05) | 엔티티는 storage 에만, storage 는 `*Repository` 어댑터만 참조하고, 어댑터는 애그리거트만 주고받는다 | ArchUnit | 영속 모델이 도메인이나 API 계약이 되지 않는다 |
| [R-06](transactions.md#R-06) | `@Transactional` 은 usecase 와 `*Reader`/`*Writer` 에만, `TransactionTemplate` 은 usecase 에만 | ArchUnit | 트랜잭션을 여는 곳이 두 종류뿐이어야 추적할 수 있다 |
| [R-07](transactions.md#R-07) | `*Reader` 는 readOnly, `*Writer` 는 쓰기 트랜잭션, 도메인 서비스는 기본 전파만 | ArchUnit | 경계 조절은 usecase 한 곳에서 한다 |
| [R-08](transactions.md#R-08) | 쓰기를 포함해 도메인 서비스를 둘 이상 부르는 usecase 메서드는 트랜잭션을 연다 | ArchUnit | 바깥 트랜잭션 누락으로 생기는 조용한 부분 커밋을 막는다 |
| [R-09](domain-services.md#R-09) | usecase 는 `*Repository`·storage 를 쓰지 않고 다른 `*UseCase` 를 부르지 않는다 | ArchUnit | 저장소 접근은 한 곳, 트랜잭션 경계는 중첩되지 않는다 |
| [R-10](api.md#R-10) | api 는 `*Repository`·`*Policy`·storage·client 를 쓰지 않고 애그리거트는 접근자만 읽는다 | ArchUnit | 트랜잭션 밖 상태 변경과 저장 누락을 막는다 |
| [R-11](api.md#R-11) | 엔드포인트 하나는 usecase·도메인 서비스 메서드 하나만 부른다 | ArchUnit | 호출마다 트랜잭션이 따로 열려 부분 커밋이 생긴다 |
| [R-12](api.md#R-12) | 요청·응답 객체는 domain 타입을 필드로 갖지 않는다 | ArchUnit | API 계약과 도메인 모델을 따로 바꿀 수 있어야 한다 |
| [R-13](structure.md#R-13) | core-enum 의 enum 은 아무것에도 의존하지 않는다 | ArchUnit | 모든 모듈이 참조하므로 의존이 전파된다 |
| [R-14](domain-model.md#R-14) | 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다 | ArchUnit | 애그리거트가 통째로 로딩·저장되는 단위를 지킨다 |
| [R-15](domain-model.md#R-15) | 도메인 모델은 public setter 를 갖지 않는다 | ArchUnit | 상태는 규칙을 가진 메서드로만 바뀐다 |
| [R-16](domain-services.md#R-16) | 애그리거트 상태를 바꾼 `*Writer` 메서드는 `*Repository.save` 를 부른다 | ArchUnit | 더티 체킹이 없어 저장 누락은 데이터 유실이다 |
| [R-17](domain-services.md#R-17) | `*Reader` 는 저장·삭제하지 않는다 | ArchUnit | 읽기 전용 트랜잭션 안의 쓰기는 반영되지 않는다 |
| [R-18](transactions.md#R-18) | `@Transactional` usecase 메서드는 client 를 부르지 않는다 | ArchUnit | 외부 I/O 동안 커넥션을 잡지 않고, 되돌릴 수 없는 호출을 롤백 범위에 넣지 않는다 |
| [R-19](api.md#R-19) | api 는 값 객체를 만들고 읽기만 하고 연산하지 않는다 | ArchUnit | 계산 규칙이 컨트롤러로 새지 않는다 |
| [R-20](domain-model.md#R-20) | 애그리거트는 public 생성자가 없고 자기 정적 팩토리로만 만든다 | ArchUnit | 생성 시점 불변식을 건너뛰지 않는다 |
| [R-21](domain-model.md#R-21) | 애그리거트의 `restore` 는 같은 이름의 `*Repository` 만 부른다 | ArchUnit | 불변식을 검사하지 않는 복원 통로로 규칙을 우회하지 않는다 |
| [S-01](domain-services.md#S-01) | usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다 | 리뷰 WARN | 같은 판단이 두 곳에 있으면 하나만 고쳐진다 |
| [S-02](domain-model.md#S-02) | 규칙이 있는 애그리거트는 빈약하지 않다 | 리뷰 WARN | 규칙이 서비스의 `if` 로 흩어지지 않는다 |
| [S-04](domain-model.md#S-04) | 여러 애그리거트에 걸친 규칙은 `*Policy` 에 둔다 | 리뷰 WARN | 저장소 없이 판단할 수 있는 규칙은 순수 객체에 있어야 한다 |
| [S-06](transactions.md#S-06) | 트랜잭션을 나눈 usecase 는 중간 상태와 재실행 안전성을 설계한다 | 리뷰 WARN | 앞 구간만 커밋된 상태를 정리·대사할 수 있어야 한다 |
| [S-07](transactions.md#S-07) | 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다 | 리뷰 BLOCK | rollback-only 로 커밋 시점에 엉뚱한 실패가 난다 |
| [S-08](domain-services.md#S-08) | 저장소 어댑터와 client 는 변환만 하고 규칙을 갖지 않는다 | 리뷰 WARN | 규칙이 도메인 모델 밖에 생기지 않는다 |

옮겨졌거나 이 템플릿에 없는 번호

- S-03 → R-19 로 옮김. 구조로 판별할 수 있게 되었다.
- S-05 → R-16 으로 옮김. 구조로 판별할 수 있게 되었다.
- S-09 통합형 전용. 애그리거트가 JPA 엔티티일 때만 의미가 있다.
