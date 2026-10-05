#!/usr/bin/env bash
# 골든 세트로 리뷰 에이전트의 재현율·정밀도를 잰다. 프롬프트를 바꿀 때마다 실행한다.
#
#   review/eval.sh                # review/golden/cases/* 전부
#   review/eval.sh s05-*          # 글로브로 일부만
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
PATTERN="${1:-*}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

for case_dir in review/golden/cases/$PATTERN/; do
    name="$(basename "$case_dir")"
    [ -f "$case_dir/diff.patch" ] || continue
    review/arch-review.sh --diff "$case_dir/diff.patch" --no-archunit --output "$OUT/$name.md" >/dev/null 2>&1 || true
    cp "$case_dir/expected.txt" "$OUT/$name.expected"
done

python3 - "$OUT" <<'PY'
import glob, os, re, sys
from collections import defaultdict
out = sys.argv[1]
tp = defaultdict(int); fp = defaultdict(int); fn = defaultdict(int)
rows = []
for exp_path in sorted(glob.glob(out + '/*.expected')):
    name = os.path.basename(exp_path)[:-9]
    expected = {l.split()[0] for l in open(exp_path, encoding='utf-8') if l.strip() and not l.startswith('#')}
    found = set()
    md = out + '/' + name + '.md'
    if os.path.exists(md):
        cur = None
        for line in open(md, encoding='utf-8'):
            m = re.match(r'\s*-\s*rule:\s*(S-\d\d)', line)
            if m: cur = m.group(1); continue
            m = re.match(r'\s*verdict:\s*(violation|suspect)', line)
            if m and cur: found.add(cur); cur = None
    for r in expected & found: tp[r] += 1
    for r in found - expected: fp[r] += 1
    for r in expected - found: fn[r] += 1
    rows.append((name, sorted(expected), sorted(found)))
print('%-40s %-18s %s' % ('case', 'expected', 'found'))
for name, e, f in rows:
    mark = 'ok ' if e == f else 'XX '
    print(mark + '%-37s %-18s %s' % (name, ','.join(e) or '-', ','.join(f) or '-'))
print()
print('%-6s %4s %4s %4s %9s %6s' % ('rule', 'tp', 'fp', 'fn', 'precision', 'recall'))
for r in sorted(set(tp) | set(fp) | set(fn)):
    p = tp[r] / (tp[r] + fp[r]) if tp[r] + fp[r] else float('nan')
    rc = tp[r] / (tp[r] + fn[r]) if tp[r] + fn[r] else float('nan')
    print('%-6s %4d %4d %4d %9.2f %6.2f' % (r, tp[r], fp[r], fn[r], p, rc))
PY
