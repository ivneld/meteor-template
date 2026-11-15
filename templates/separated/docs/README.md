# 문서

사람이 읽는 문서다. AI 에이전트는 저장소 루트의 `AGENTS.md` 와 `harness/` 를 본다. 두 쪽은 같은 규칙 ID 를 쓴다.

| 문서 | 언제 보는가 |
|---|---|
| [architecture/](architecture/README.md) | 설계 배경, 코드를 어디에 둘지, usecase 가 필요한지, 트랜잭션을 묶거나 나눌 때, 새 기능을 추가할 때 |
| [architecture/rules.md](architecture/rules.md) | 규칙 ID 로 규칙을 찾을 때, ArchUnit 이나 리뷰 에이전트가 지적한 규칙의 이유를 볼 때 |
| [error-handling.md](error-handling.md) | 에러 코드를 추가하거나 예외를 던질 때, 오류 응답 형식을 알아야 할 때 |
| [scaling.md](scaling.md) | 트래픽·코드가 커져 구조를 바꿔야 하는지, 배치 같은 두 번째 실행 모듈을 만들 때, 다른 앱과 연동할 때 |
| [review-agent.md](review-agent.md) | 코드리뷰 요청 전 `/arch-review` 를 돌릴 때, 결과를 해석할 때 |
