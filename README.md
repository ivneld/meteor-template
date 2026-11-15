# meteor-template

Spring Boot(Java) 애플리케이션을 시작할 때 복사해 쓰는 템플릿의 원본 보관소다. 이 저장소 자체는 애플리케이션이 아니다. 두 템플릿이
`templates/` 아래에 각자 독립된 Gradle 프로젝트로 들어 있고, 새 프로젝트는 그중 하나를 복사해 시작한다.

| 템플릿 | 위치 | 한 줄 |
|---|---|---|
| 분리형 | [templates/separated](templates/separated/README.md) | 애그리거트와 JPA 엔티티를 다른 타입으로 둔다. 도메인 모델은 JPA 를 모른다 |
| 통합형 | [templates/unified](templates/unified/README.md) | 애그리거트가 곧 JPA 엔티티다. 나중에 분리할 수 있는 상태를 규칙으로 지킨다 |

두 템플릿은 같은 설계 원칙을 공유한다. 계층은 `domain`, `usecase`, `api` 셋이고, 규칙은 도메인 모델 한 곳에 두며, 트랜잭션 경계는 코드에서
보인다. 다른 점은 애그리거트와 JPA 엔티티를 분리하는가 하나다.

## 어느 템플릿을 고르나

판단 기준은 "도메인이 복잡한가"보다 **도메인 모델과 저장 모델의 모양이 얼마나 다르고, 그 모델을 처음부터 알고 있는가**다.

### 분리형: 규칙이 처음부터 풍부하고 미리 알려진 도메인

X.509 PKI, V2X SCMS, 결제·정산 코어처럼 표준이나 명세가 도메인 규칙을 먼저 정해 둔 도메인이다.

- 모델을 처음부터 안다. 요구사항 때문에 모델을 갈아엎는 일이 드물어서 순수 도메인 모델에 들인 비용을 오래 회수한다.
- 도메인과 저장의 모양이 원래 다르다. 불변이고 값 객체 중심인 모델을 DER blob, 조회용 추출 컬럼, 지연 로딩하는 본문으로 저장한다. 한
  클래스로 두 모양을 함께 만족시키려 하면 어느 한쪽이 휜다.
- JPA 를 모르는 모델이라 두 번째 실행 모듈이나 다른 애플리케이션과 공유하기 쉽다.

### 통합형: 요구사항이 운영하며 드러나는 B2C 서비스와 PoC

일단 PoC 수준으로 만든 뒤 운영하면서 요구사항이 분명해지는 서비스다.

- 요구사항이 자주 바뀌는 시기에는 엔티티, 애그리거트, 매퍼를 매번 같이 고쳐야 해서 변경 비용이 두세 배가 된다.
- 아직 모르는 모양을 저장 모델과 분리해 두면 잘못된 추상화가 두 겹으로 굳는다.
- 대신 나중에 분리할 수 있는 규율을 지킨다. 애그리거트끼리는 ID 로만 참조하고 연관관계를 걸지 않는다(R-14). setter 를 두지 않는다(R-15).
  바꾸면 save 를 명시한다(R-16). 정적 팩토리로만 만든다(R-20). `EntityManager` 를 쓰지 않는다(R-05). 이 규율이 없으면 "나중에 분리"는 오지
  않는다.
- 분리형으로 옮길 신호: 지연 로딩·cascade·더티 체킹이 설계를 좌우하기 시작한다, 도메인 규칙 테스트에 DB 가 필요해진다, 같은 애그리거트에
  저장 모양이 둘 이상 필요해진다, JPA 의 요구 때문에 불변식을 지키기 어려워진다, 도메인을 다른 모듈·앱과 공유해야 한다.

### 판단할 때 함께 볼 것

- **분리하지 않아도 매핑 비용은 사라지지 않고 자리를 옮긴다.** 엔티티를 API 응답으로 그대로 내보내면 스키마와 API 계약이 묶이므로, 통합형도
  요청·응답 객체를 따로 둔다(R-12). 통합형이 아끼는 것은 엔티티 ↔ 애그리거트 매핑 하나다.
- **애그리거트 단위로 섞을 수 있다.** 통합형으로 시작해 결제처럼 규칙이 명확하고 틀리면 비싼 애그리거트만 분리형으로 옮길 수 있다. 다만
  템플릿은 한 방식으로 일관되게 시작한다. 섞는 것은 신호가 보인 애그리거트에 한해, ADR 을 남기고 한다.
- 판단이 서지 않으면 통합형으로 시작한다. 규율을 지켰다면 분리형으로 옮기는 일은 애그리거트마다 기계적이다(통합형의 `docs/scaling.md`).

## 두 템플릿의 차이

| | 분리형 | 통합형 |
|---|---|---|
| 모듈 | `storage/db-core` 에 JPA 엔티티(`com.meteor.storage`)와 저장소 인프라 | `storage/db-core` 에 저장소 인프라만(`BaseEntity`, DataSource, JPA 설정) |
| 애그리거트 | 순수 자바. `final` 필드, private 생성자, 정적 팩토리, `restore` | `@Entity`, `BaseEntity` 상속, `protected` 기본 생성자, 정적 팩토리 |
| `*Repository` | Spring Data 를 감싸 엔티티 ↔ 애그리거트를 변환하는 구체 클래스 | `JpaRepository` 를 상속한 인터페이스 |
| 값 객체 | record. 엔티티에는 펼친 컬럼으로 저장 | `@Embeddable` record. `@AttributeOverride` 로 컬럼 이름 지정 |
| 저장 | 더티 체킹이 없어 save 가 필수 | 더티 체킹이 있어도 save 를 명시 |
| 테스트에서 특정 상태 만들기 | `Order.restore(...)` | 팩토리 + 상태 전이 메서드, 식별자는 `Persisted.withId(...)` |
| 규칙 차이 | R-05 엔티티는 storage 에만, 어댑터는 애그리거트만 주고받는다. R-21 `restore` 는 같은 이름의 `*Repository` 만. S-08 어댑터는 변환만 | R-05 엔티티는 domain 의 애그리거트, `EntityManager` 금지. R-21 없음. S-08 쿼리에 규칙을 넣지 않는다. S-09 JPA 가 도메인 설계를 정하지 않는다 |

그 밖의 규칙(R-01 ~ R-20, S-01, S-02, S-04, S-06, S-07)은 같은 번호로 같은 개념을 가리킨다. R-01, R-02 는 영속 의존의 허용 범위만
다르다. 각 템플릿의 `docs/architecture/rules.md` 가 전체 목록이다.

## 새 프로젝트 시작하기

스크립트 없이 손으로 시작한다.

1. 고른 템플릿 디렉터리를 저장소 밖의 새 경로로 복사한다.

   ```bash
   cp -R templates/unified ~/Projects/my-app
   ```

2. `settings.gradle` 의 `rootProject.name` 을 바꾼다.
3. 패키지를 바꾼다. `gradle.properties` 의 `projectGroup` 과 모든 모듈의 `com.meteor` 패키지(디렉터리와 `package` 선언), 그리고
   `ArchitectureRules` 의 패키지 문자열(`com.meteor.domain..` 등)을 함께 바꾼다. 하네스 문서(`AGENTS.md`, `harness/`, `.claude/rules/`)와
   `docs/` 의 경로 표기도 함께 바꾼다.
4. 레퍼런스 도메인(`member`, `order`, `payment`)은 첫 기능을 만들 때까지 본보기로 두고, 필요 없어지면 지운다. 지울 때 `ArchitectureRulesSelfTest`
   의 fixture 는 남긴다.
5. `git init` 하고 `./gradlew checkFormat test` 로 확인한다.

## 이 저장소를 고칠 때

- 두 템플릿은 서로 독립된 프로젝트다. 각 디렉터리에서 따로 빌드한다(`cd templates/separated && ./gradlew test`).
- 공통 경로(`support/`, `clients/`, `tests/`, `gradle/`, 빌드 스크립트, `docs/error-handling.md` 등)는 두 템플릿을 함께 고친다.
  `scripts/check-common.sh` 가 공통 경로가 같은지 CI 에서 검사한다.
- 템플릿마다 다른 부분(도메인, 저장소, `ArchitectureRules`, 규칙 문서, 하네스)은 그 템플릿 안에서 고친다. 같은 규칙을 두 템플릿에서 바꾸면
  같은 번호로 두 쪽 모두 고친다.
- CI(`.github/workflows/ci.yml`)는 두 템플릿을 각각 `checkFormat test` 한다. 각 템플릿 안의 `.github/` 는 복사해 간 새 프로젝트에서 쓰인다.
