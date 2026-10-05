# 리뷰 에이전트 하네스

코드리뷰를 요청하기 전에 팀원이 직접 돌리는 아키텍처 리뷰. [Pi](https://github.com/badlogic/pi-mono) 를 읽기 전용 에이전트로 실행해
[ARCHITECTURE.md](ARCHITECTURE.md) 의 의미 규칙(S-xx)을 검사한다. 구조 규칙(R-xx)은 ArchUnit 이 맡는다.

## 실행

```bash
npm i -g @mariozechner/pi-coding-agent   # 최초 1회. 이후 pi 를 한 번 실행해 /login 으로 모델 제공자 인증
review/arch-review.sh                     # origin/main 과의 차이(커밋 + 작업 트리)를 검토
review/arch-review.sh --base main         # 비교 기준 변경
PI_REVIEW_MODEL=anthropic/claude-sonnet-4-5 review/arch-review.sh   # 모델 지정. 비우면 pi 기본 모델
```

| 종료 코드 | 의미 | 처리 |
|---|---|---|
| 0 | PASS. findings 없음 | 리뷰를 요청한다 |
| 1 | BLOCK. S-04, S-05, S-06 위반 | 고치거나 ADR 로 예외를 기록한 뒤 다시 돌린다 |
| 2 | WARN. 그 외 위반 또는 suspect | 판단해서 고치거나 리뷰 요청에 사유를 적는다 |
| 3 | 실행 오류 (pi 없음, 인증 실패 등) | 메시지를 본다 |

푸시 때마다 자동으로 돌리려면 `git config core.hooksPath .githooks` 로 훅을 켠다. BLOCK 만 푸시를 막고, `SKIP_ARCH_REVIEW=1 git push` 로 건너뛸 수 있다.
대화형으로 Pi 를 쓰는 중이라면 `/skill:arch-review` 로 같은 절차를 부른다.

## 하네스 구성

```
review/
├── arch-review.sh        입력 수집(diff, ArchUnit 결과, 변경 파일) → pi 실행 → 출력 파싱 → 종료 코드
├── eval.sh               골든 세트로 재현율·정밀도 측정
├── prompts/
│   ├── reviewer.md       시스템 프롬프트. 역할, 검사 범위(S-xx 만), 작업 방식, 출력 형식
│   └── task.md           사용자 프롬프트. 첨부 파일의 읽는 순서
└── golden/cases/<이름>/  diff.patch + expected.txt
.agents/skills/arch-review/SKILL.md   대화형 Pi 용 스킬. 같은 프롬프트를 가리킨다
.githooks/pre-push                    선택적 훅
```

`arch-review.sh` 가 pi 를 부르는 방식과 그 이유.

| 옵션 | 이유 |
|---|---|
| `-p --no-session` | 비대화형, 세션을 남기지 않는다 |
| `--tools read,grep,find,ls` | 읽기 전용. 리뷰어는 코드를 고치지 않는다 |
| `-nc -ne -ns -np` | AGENTS.md, 확장, 스킬, 템플릿 자동 탐색을 끈다. 입력은 스크립트가 넘기는 것뿐이어야 결과가 재현된다 |
| `--append-system-prompt review/prompts/reviewer.md` | 역할과 출력 형식 |
| `@docs/ARCHITECTURE.md @docs/REVIEW_AGENT.md @archunit.txt @changed-files.txt @diff.patch` | 규칙 문서, 판단 가이드, ArchUnit 결과, 변경 목록, 변경 본문 |

에이전트는 diff 만으로 판단이 안 되면 read/grep 으로 같은 컨텍스트의 파일을 직접 읽는다. 그래서 저장소 루트에서 실행해야 한다.

## 역할 분담

| 수단 | 맡는 규칙 | 성격 |
|---|---|---|
| ArchUnit (`./gradlew test`) | R-01 ~ R-09 | 결정적. 전체 코드 그래프를 본다. 위반이면 머지 불가 |
| 리뷰 에이전트 | S-01 ~ S-09 | 의미 판단. PR diff 와 주변 코드를 본다. 규칙별로 차단/권고를 정한다 |
| 사람 | 설계 판단 | 경계가 맞는지, 규칙 자체를 바꿔야 하는지 |

## 에이전트 입력

1. 검토 대상 diff 전체 (`git merge-base origin/main HEAD` 기준, 작업 트리 포함)
2. 변경 파일 목록
3. `docs/ARCHITECTURE.md` 와 이 문서의 판단 가이드
4. ArchUnit 실행 결과 (실패한 규칙 ID 목록). 실패가 있으면 그 맥락에서 의미 위반을 찾는다
5. 같은 컨텍스트의 나머지 파일은 에이전트가 read/grep 으로 직접 읽는다. 스크립트가 미리 모아 주지 않는다

## 프롬프트

시스템 프롬프트는 `review/prompts/reviewer.md`, 요청 프롬프트는 `review/prompts/task.md` 가 단일 출처다. 여기에 복사해 두지 않는다.
출력은 `findings:` 목록(rule, file, line, verdict, evidence, why, fix)과 마지막 줄 `result: PASS|WARN|BLOCK` 으로 고정되어 있고,
`arch-review.sh` 와 `eval.sh` 가 그 형식을 파싱한다. 형식을 바꾸면 두 스크립트의 파서도 함께 바꾼다.

`why` 에는 규칙의 이유를 ARCHITECTURE.md 의 표현으로 쓰게 했다. 코멘트가 쌓이면 규칙이 팀의 언어가 된다.

## 규칙별 판단 가이드

에이전트가 각 규칙을 어떻게 확인하는지. 골든 세트의 기대 결과도 이 기준으로 만든다.

| 규칙 | 확인 방법 | 오탐 주의 |
|---|---|---|
| S-01 | `*UseCase` 본문에서 도메인 객체의 getter 결과로 분기하는 `if`/`switch` | 입력 검증(null, 빈 값)은 위반 아님 |
| S-02 | `domain` 의 애그리거트에 public setter, 상태 변경 메서드 부재, UseCase 가 `restore(...)` 로 새 상태를 조립 | record VO 는 대상 아님 |
| S-03 | `*Controller` 에서 `of`/`from`/getter 외의 VO 메서드 호출 | Request → Command 변환은 허용 |
| S-04 | `domain` 클래스 필드 타입이 다른 컨텍스트의 애그리거트. `storage` 엔티티의 `@ManyToOne` 등이 다른 컨텍스트 엔티티 | ID 타입(VO) 보유는 허용 |
| S-05 | `*UseCase` 생성자가 두 컨텍스트 이상의 `*Repository`/`*Facade` 를 주입 | 조회 전용 Facade 하나를 읽는 것은 권고 수준 |
| S-06 | `@Query`, 네이티브 쿼리, JPQL 에 다른 컨텍스트 테이블·엔티티 등장 | 같은 컨텍스트 내부 JOIN 은 허용 |
| S-07 | `@TransactionalEventListener` 의 phase 가 `BEFORE_COMMIT`, 리스너가 반환값을 호출자에게 돌려줌, 리스너가 부르는 UseCase 메서드에 `REQUIRES_NEW` 가 없음 | `@EventListener` 동기 리스너는 같은 컨텍스트 내부라면 허용 |
| S-08 | `storage`/`clients` 클래스 안의 도메인 상태 분기, 계산 | null 처리와 타입 변환은 허용 |
| S-09 | UseCase 가 애그리거트 메서드를 호출한 뒤 `save()` 없이 반환 | 읽기 전용 UseCase 는 대상 아님 |

## 차단 범위

- 차단(머지 불가): S-04, S-05, S-06. 분리 비용을 직접 만드는 규칙이고 오탐이 거의 없다.
- 권고(코멘트만): 나머지. 골든 세트에서 정밀도가 90% 를 넘으면 차단으로 올린다.

오탐이 차단하기 시작하면 팀은 에이전트를 무시하는 법을 배운다. 차단 범위는 측정 결과로만 넓힌다.

## 골든 세트

프롬프트를 바꿀 때마다 재현율과 정밀도를 재기 위한 고정 입력. `review/golden/cases/<이름>/` 에 `diff.patch` 와 `expected.txt` 를 두고
`review/eval.sh` 로 돌린다. 만드는 법은 [review/golden/README.md](../review/golden/README.md).

시작 케이스 다섯 개: S-01, S-05, S-07, S-09 위반 각 1개와 위반처럼 보이지만 아닌 입력 검증 1개.
실제 PR 에서 에이전트가 놓치거나 오탐한 사례를 케이스로 추가한다. 측정은 규칙별 재현율·정밀도이며, 차단 규칙은 정밀도 우선, 권고 규칙은 재현율 우선.

## 운영

- 규칙 변경은 `ARCHITECTURE.md` PR 하나로 하고, 같은 PR 에서 ArchUnit 테스트와 이 문서를 고친다.
- 에이전트 코멘트에 반박이 있으면 사람이 판단하고, 결과를 골든 세트에 넣는다. 에이전트와 논쟁하지 않는다.
- 분기마다 ARCHITECTURE.md 의 측정 항목과 함께 골든 세트 점수를 본다.
