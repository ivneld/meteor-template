# 리뷰 에이전트

코드리뷰를 요청하기 전에 팀원이 직접 돌리는 아키텍처 리뷰다. [Pi](https://github.com/badlogic/pi-mono) 를 읽기 전용 에이전트로 실행해
의미 규칙(S-xx)을 검사한다. 구조 규칙(R-xx)은 ArchUnit 이 맡는다. 규칙 목록은 [규칙 인덱스](architecture/rules.md).

## 역할 분담

| 수단 | 맡는 규칙 | 성격 |
|---|---|---|
| ArchUnit (`./gradlew test`) | R-xx | 결정적. 전체 코드 그래프를 본다. 위반이면 머지 불가 |
| 리뷰 에이전트 (`/arch-review`) | S-xx | 의미 판단. PR diff 와 주변 코드를 본다. S-07 만 BLOCK, 나머지는 WARN |
| 사람 | 설계 판단 | usecase 가 필요한지, 트랜잭션을 나누는 게 맞는지, 규칙 자체를 바꿔야 하는지 |

구조로 판별할 수 있게 된 규칙은 ArchUnit 으로 옮긴다. 저장 누락(구 S-05)은 R-16, 컨트롤러의 값 객체 연산(구 S-03)은 R-19,
`@Transactional` 안의 외부 호출(구 S-06 의 일부)은 R-18 이 되었다.

## 설치와 설정

1. **Pi 설치** (1.x)

   ```bash
   npm i -g @earendil-works/pi-coding-agent
   pi --version   # 1.0.x
   ```

2. **모델과 토큰을 `.env` 에 적는다.** `.env.example` 을 복사한다. `.env` 는 gitignore 되어 있다.

   ```bash
   cp .env.example .env
   ```

   ```dotenv
   ARCH_REVIEW_MODEL=anthropic/claude-sonnet-4-5   # 리뷰 턴에만 쓸 모델. provider/model 형식. 비우면 현재 모델
   ANTHROPIC_API_KEY=sk-ant-...                     # 쓰는 제공자의 토큰. pi 가 읽는 환경변수 이름 그대로
   ```

   다른 제공자를 쓰면 그 제공자의 변수 이름으로 바꾼다(`OPENAI_API_KEY`, `GEMINI_API_KEY`, `OPENROUTER_API_KEY` 등). `/arch-review` 는
   시작할 때 `.env` 를 읽어 **아직 없는 환경변수만** 채우므로, 셸에 설정된 값이나 `pi` 의 `/login` 인증이 있으면 그쪽이 우선한다.

3. **프로젝트를 한 번 신뢰한다.** `/arch-review` 는 저장소의 `.pi/extensions` 에 있어서 Pi 의 프로젝트 신뢰가 필요하다.

   ```bash
   pi          # 저장소 루트에서 실행하면 신뢰 여부를 묻는다
   /trust      # 이미 열려 있다면 이 명령으로 저장
   ```

   비대화형(`-p`)은 물어볼 수 없으므로 저장된 결정이 없으면 확장을 조용히 건너뛴다. 훅이나 스크립트에서는 항상 `--approve` 를 붙인다.

## 리뷰 받기

```bash
# 대화형: pi 를 띄우고 프롬프트에 입력
/arch-review                       # origin/main 과의 차이(커밋 + 작업 트리)를 검토
/arch-review --base main           # 비교 기준 변경
/arch-review --no-archunit         # ArchUnit 생략
/arch-review --diff x.patch        # 주어진 패치를 검토 (골든 세트 평가용)

# 비대화형: 결과를 출력하고 종료 코드로 판정을 알린다
pi --approve -p --no-session "/arch-review" < /dev/null
```

| 종료 코드 (`-p`) | 의미 | 처리 |
|---|---|---|
| 0 | PASS. findings 없음 | 리뷰를 요청한다 |
| 1 | BLOCK. S-07 위반 | 고치거나 ADR 로 예외를 기록한 뒤 다시 돌린다 |
| 2 | WARN. 그 외 위반 또는 suspect | 판단해서 고치거나 리뷰 요청에 사유를 적는다 |

결과 본문은 `findings:` 목록과 `result:` 한 줄이다. 각 finding 에는 규칙 ID, 파일과 줄, 근거 코드, 규칙의 이유(`why`), 수정 방향(`fix`)이
있다.

비대화형으로 쓸 때 `< /dev/null` 과 `--approve` 를 붙인다. Pi 는 stdin 이 터미널이 아니면 그 내용을 EOF 까지 기다리고, 신뢰 결정이 없으면
확장을 건너뛴다.

푸시 때마다 자동으로 돌리려면 `git config core.hooksPath .githooks` 로 훅을 켠다. BLOCK 만 푸시를 막고, `SKIP_ARCH_REVIEW=1 git push`
로 건너뛸 수 있다. pi 가 없는 기기에서는 훅이 리뷰를 건너뛴다.

## 하네스 파일

| 파일 | 내용 |
|---|---|
| `.pi/extensions/arch-review.ts` | `/arch-review` 명령. 입력 수집, 그 턴만 읽기 전용 도구와 리뷰 모델 적용, 판정과 종료 코드 |
| `harness/review/prompts/reviewer.md` | 시스템 프롬프트. 역할, 검사 범위(S 규칙만), 출력 형식 |
| `harness/review/prompts/task.md` | 요청 프롬프트. 입력을 읽는 순서 |
| `harness/rules/S-*.md` | 규칙별 위반 신호와 오탐 주의. 에이전트가 판단 기준으로 쓴다 |
| `harness/review/golden/` | 골든 세트. 케이스마다 `diff.patch` 와 `expected.txt` |
| `harness/review/eval.sh` | 골든 세트로 규칙별 재현율·정밀도를 잰다 |
| `.githooks/pre-push` | 선택적 푸시 훅 |

규칙별 판단 기준은 `harness/rules/S-*.md` 가 단일 출처다. 이 문서에 복사해 두지 않는다. `ARCH_REVIEW_DEBUG=1` 을 주면 실제로 보낸
프롬프트가 `build/arch-review-prompt.md` 에 남는다.

## 골든 세트

프롬프트나 규칙 파일을 바꿀 때마다 재현율과 정밀도를 재기 위한 고정 입력이다.

```bash
harness/review/eval.sh              # 전부
harness/review/eval.sh 's07-*'      # 글로브로 일부만
```

- 규칙마다 위반 케이스 하나 이상, 위반처럼 보이지만 아닌 케이스 하나 이상을 둔다.
- 실제 PR 에서 에이전트가 놓치거나 오탐한 사례를 케이스로 추가한다.
- 차단 규칙(S-07)은 정밀도 우선, 권고 규칙은 재현율 우선으로 본다.

## 차단 범위

차단(머지 불가)은 S-07 하나다. 트랜잭션 안에서 삼킨 예외는 커밋 시점에 엉뚱한 실패를 내고, 오탐이 거의 없다. 나머지는 권고다. 오탐이
차단하기 시작하면 팀은 에이전트를 무시하는 법을 배운다. 차단 범위는 골든 세트의 측정 결과로만 넓힌다.

## 운영

- 규칙 변경은 한 PR 에서 [규칙 인덱스](architecture/rules.md), 해당 장, `ArchitectureRules`, `harness/rules/` 를 함께 고친다.
- 에이전트 코멘트에 반박이 있으면 사람이 판단하고 결과를 골든 세트에 넣는다. 에이전트와 논쟁하지 않는다.
- 분기마다 [측정 항목](architecture/changing-rules.md#측정)과 함께 골든 세트 점수를 본다.
