# AGENTS.md — meteor-template 저장소

이 저장소는 애플리케이션이 아니라 템플릿 원본 보관소다. 두 템플릿이 각자 독립된 Gradle 프로젝트로 들어 있다.

| 템플릿 | 위치 | 진입점 |
|---|---|---|
| 분리형 (애그리거트 ≠ JPA 엔티티) | `templates/separated/` | `templates/separated/AGENTS.md` |
| 통합형 (애그리거트 = JPA 엔티티) | `templates/unified/` | `templates/unified/AGENTS.md` |

- 코드를 고칠 때는 고치는 템플릿의 `AGENTS.md` 를 따른다. 빌드와 테스트는 그 템플릿 디렉터리에서 돌린다(`cd templates/<이름> && ./gradlew checkFormat test`).
- 공통 경로(빌드 스크립트, `support/`, `clients/`, `tests/`, 도메인 서비스, 에러 처리, 리뷰 하네스 등. 목록은 `scripts/check-common.sh`)는 두 템플릿을 함께 고친다. 끝내기 전에 `scripts/check-common.sh` 를 돌린다.
- 같은 규칙(같은 ID)을 바꾸면 두 템플릿 모두에서 네 곳(ArchUnit, `harness/rules`, `AGENTS.md`, `docs/architecture/rules.md`)을 함께 고친다.
- 어떤 템플릿을 고를지에 대한 설명은 `README.md`.
