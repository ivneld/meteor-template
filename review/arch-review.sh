#!/usr/bin/env bash
# 아키텍처 컨벤션 리뷰. 팀원이 코드리뷰를 요청하기 전에 실행한다.
#
#   review/arch-review.sh                 # origin/main 과의 차이(커밋 + 작업 트리)를 검토
#   review/arch-review.sh --base main     # 비교 기준 변경
#   review/arch-review.sh --diff x.patch  # 주어진 패치를 검토 (골든 세트 평가용)
#
# 종료 코드: 0 PASS, 1 BLOCK (S-04/S-05/S-06 위반), 2 WARN (그 외 위반 또는 suspect), 3 실행 오류
#
# 환경 변수:
#   PI_REVIEW_MODEL   pi 에 넘길 --model 값 (예: anthropic/claude-sonnet-4-5). 비우면 pi 기본 모델
#   PI_BIN            pi 실행 파일 경로 (기본: PATH 의 pi)
set -euo pipefail

BASE="origin/main"
DIFF_FILE=""
RUN_ARCHUNIT=1
OUTPUT=""
while [ $# -gt 0 ]; do
    case "$1" in
        --base) BASE="$2"; shift 2 ;;
        --diff) DIFF_FILE="$2"; shift 2 ;;
        --no-archunit) RUN_ARCHUNIT=0; shift ;;
        --output) OUTPUT="$2"; shift 2 ;;
        -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
        *) echo "unknown option: $1" >&2; exit 3 ;;
    esac
done

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
PI="${PI_BIN:-pi}"
if ! command -v "$PI" >/dev/null 2>&1; then
    echo "pi 가 없습니다. 설치: npm i -g @mariozechner/pi-coding-agent  (그 다음 pi 를 실행해 /login 으로 인증)" >&2
    exit 3
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# 1. 검토 대상 diff
if [ -n "$DIFF_FILE" ]; then
    cp "$DIFF_FILE" "$WORK/diff.patch"
else
    MERGE_BASE="$(git merge-base "$BASE" HEAD)"
    git diff "$MERGE_BASE" -- . ':(exclude)**/build/**' ':(exclude)gradle/wrapper/**' > "$WORK/diff.patch"
fi
if [ ! -s "$WORK/diff.patch" ]; then
    echo "검토할 변경이 없습니다. (base: $BASE)"
    exit 0
fi
grep '^+++ b/' "$WORK/diff.patch" | sed 's|^+++ b/||' > "$WORK/changed-files.txt" || true

# 2. ArchUnit 결과
if [ "$RUN_ARCHUNIT" = 1 ]; then
    ./gradlew -q :core:core-api:test --tests '*ArchitectureRules*' >/dev/null 2>&1 || true
    python3 - "$ROOT/core/core-api/build/test-results/test" > "$WORK/archunit.txt" <<'PY'
import glob, sys, xml.etree.ElementTree as E
failed = []
for f in glob.glob(sys.argv[1] + '/*ArchitectureRules*.xml'):
    for tc in E.parse(f).getroot().iter('testcase'):
        if tc.find('failure') is not None:
            failed.append(tc.get('name'))
if failed:
    print('ArchUnit FAILED rules:')
    for n in failed: print(' -', n)
else:
    print('ArchUnit: all R rules passed')
PY
else
    echo "ArchUnit: skipped" > "$WORK/archunit.txt"
fi

# 3. pi 실행 (읽기 전용 도구만, 세션 저장 없음, 프로젝트 자동 설정 비활성)
MODEL_ARGS=()
[ -n "${PI_REVIEW_MODEL:-}" ] && MODEL_ARGS=(--model "$PI_REVIEW_MODEL")
"$PI" -p --no-session -nc -ne -ns -np \
    --tools read,grep,find,ls \
    --append-system-prompt review/prompts/reviewer.md \
    "${MODEL_ARGS[@]}" \
    @docs/ARCHITECTURE.md @docs/REVIEW_AGENT.md \
    @"$WORK/archunit.txt" @"$WORK/changed-files.txt" @"$WORK/diff.patch" \
    "$(cat review/prompts/task.md)" > "$WORK/review.md" 2> "$WORK/pi.err" || {
        echo "pi 실행 실패:" >&2; cat "$WORK/pi.err" >&2; exit 3; }

[ -n "$OUTPUT" ] && cp "$WORK/review.md" "$OUTPUT"
cat "$WORK/review.md"
echo
echo "--- ArchUnit"; cat "$WORK/archunit.txt"

# 4. 판정
python3 - "$WORK/review.md" <<'PY'
import re, sys
text = open(sys.argv[1], encoding='utf-8').read()
blocking = {'S-04', 'S-05', 'S-06'}
findings = []
cur = None
for line in text.splitlines():
    m = re.match(r'\s*-\s*rule:\s*(S-\d\d)', line)
    if m:
        cur = {'rule': m.group(1), 'verdict': ''}; findings.append(cur); continue
    m = re.match(r'\s*verdict:\s*(violation|suspect)', line)
    if m and cur is not None:
        cur['verdict'] = m.group(1)
block = any(f['rule'] in blocking and f['verdict'] == 'violation' for f in findings)
warn = any(f['verdict'] in ('violation', 'suspect') for f in findings)
print('--- 판정:', 'BLOCK' if block else 'WARN' if warn else 'PASS',
      '(%d findings)' % len(findings))
sys.exit(1 if block else 2 if warn else 0)
PY
