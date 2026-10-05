# meteor-template

Spring Boot(Java) 서비스를 팀이 함께 개발하기 위한 멀티모듈 템플릿.

- 배포 단위는 하나, 바운디드 컨텍스트는 여러 개. 코드는 컨텍스트(`sample`, `order` ...) 단위로 패키징된다.
- 컨텍스트 안은 `domain / application / api / storage` 네 계층. 도메인 규칙은 `core-domain` 에만, 트랜잭션 경계는 `application` 에만 있다.
- 컨텍스트끼리는 `*Facade` 와 `*Event` 로만 협력한다. 이 경계들은 ArchUnit 테스트와 리뷰 에이전트가 강제한다.

## 모듈 구성

```
meteor-template
├── core
│   ├── core-shared     모든 계층이 공유하는 값 타입. record(VO) 와 enum 만. 외부 의존 0
│   ├── core-domain     애그리거트, 도메인 서비스, 에러 어휘(ErrorCode, CoreException). Spring/JPA 를 모름
│   └── core-api        유일한 실행 모듈. application(UseCase) + api(Controller) + support(설정, 어드바이스)
├── storage
│   └── db-core         Repository 인터페이스와 JPA 구현. 엔티티는 이 안에서만 존재
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

### 의존 방향

```
        api ──► application ──► domain ◄── storage / clients
         │           │            ▲              │
         └───────────┴────────────┴──────────────┴──► core-shared
```

`core-domain` 은 아무것도 모른다. `storage` 는 도메인 객체를 주고받는 `*Repository` 를 소유·구현하므로 도메인에 의존한다.
`core-api` 만 전부를 안다. 실행 모듈이 얇기 때문에 배치나 다른 API 모듈을 추가할 때 `core-domain` 과 `storage` 를 그대로 재사용한다.

## 핵심 규칙

- `domain` 은 Spring, JPA, 다른 계층을 모른다.
- `@Transactional` 은 `application` 에만 있다.
- 컨텍스트끼리 직접 의존하지 않는다. `*Facade` 와 `*Event` 로만.

전체 규칙과 이유는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). 구조 규칙은 `./gradlew test` 의 `ArchitectureRules` 가 검사한다.

## 실행

```bash
./gradlew :core:core-api:bootRun
curl localhost:8080/health
curl -X POST localhost:8080/api/v1/samples -H 'Content-Type: application/json' -d '{"name":"meteor"}'
curl localhost:8080/api/v1/samples/1
curl -X POST localhost:8080/api/v1/samples/1/deactivate
```

```bash
./gradlew test                       # CI 기본. ArchUnit 포함
./gradlew :core:core-api:asciidoctor # REST Docs HTML 생성 (restDocsTest 선행)
./gradlew checkFormat                # 코드 포맷 검사
./gradlew format                     # 코드 포맷 적용
```

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
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 코드를 어디에 둘지, 규칙을 어겨야 할 때, 새 컨텍스트를 만들 때, 경계가 맞는지 점검할 때 |
| [docs/ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | 에러 코드를 추가하거나 예외를 던질 때, 오류 응답 형식을 알아야 할 때 |
| [docs/SCALING.md](docs/SCALING.md) | 트래픽·조직이 커져 구조를 바꿔야 하는지 판단할 때 |
| [docs/REVIEW_AGENT.md](docs/REVIEW_AGENT.md) | 리뷰 에이전트를 설정하거나 규칙 검사 결과를 해석할 때 |
