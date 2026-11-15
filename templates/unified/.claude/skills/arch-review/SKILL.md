---
name: arch-review
description: 이 저장소의 아키텍처 의미 규칙(S-xx)으로 현재 변경을 리뷰한다. "아키텍처 리뷰", "규칙 검사", "코드리뷰 요청 전 확인", "/arch-review" 요청에 사용한다. 코드를 고치지 않고 findings 만 보고한다.
---

# 아키텍처 리뷰 (Claude Code)

Pi 의 `/arch-review` 확장(`.pi/extensions/arch-review.ts`)과 같은 절차를 Claude Code 에서 수동으로 따른다. 판정 기준과 출력 형식의 단일 출처는 `harness/review/prompts/reviewer.md` 다.

1. `harness/review/prompts/reviewer.md` 의 역할, 작업 방식, 출력 형식을 그대로 따른다. 파일을 수정하지 않는다.
2. 입력을 모은다.
   - 규칙 인덱스: `AGENTS.md`. 검사 대상은 `강제` 가 `리뷰` 인 S 규칙이다.
   - 구조: `harness/context/structure.md`
   - 규칙: `harness/rules/S-*.md` 전부 (위반 신호, 위반 아님, review_verdict)
   - ArchUnit 결과: `./gradlew -q :core:core-api:test --tests '*ArchitectureRules*'`. 실패한 R 규칙은 다시 지적하지 않는다.
   - 변경: 사용자가 기준을 주지 않으면 `git diff $(git merge-base origin/main HEAD) -- .` (작업 트리 포함)
3. 관련 애그리거트, `*Policy`, `*Reader`/`*Writer`, `*UseCase` 파일을 읽어 판단한다.
4. 출력은 `findings:` 목록과 마지막 줄 `result: PASS|WARN|BLOCK` 뿐이다. `review_verdict: BLOCK` 규칙의 violation 이 있으면 BLOCK.
