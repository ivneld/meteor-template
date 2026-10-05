---
name: arch-review
description: 이 저장소의 아키텍처 의미 규칙(S-01~S-09)으로 현재 변경을 리뷰한다. "아키텍처 리뷰", "규칙 검사", "코드리뷰 요청 전 확인" 요청에 사용한다.
---

# 아키텍처 리뷰

비대화형 실행은 `review/arch-review.sh` 가 정답이다. 대화 중에 요청받았다면 같은 절차를 따른다.

1. `review/prompts/reviewer.md` 를 읽고 그 역할과 출력 형식을 그대로 따른다.
2. `docs/ARCHITECTURE.md` 의 S 규칙과 `docs/REVIEW_AGENT.md` 의 규칙별 판단 가이드를 읽는다.
3. 변경은 `git diff $(git merge-base origin/main HEAD)` 로 얻는다. 사용자가 범위를 지정하면 그것을 쓴다.
4. diff 만으로 판단이 안 되면 같은 컨텍스트의 domain, application, storage 파일을 읽는다. 파일을 수정하지 않는다.
5. `reviewer.md` 의 출력 형식으로만 답한다.
