@AGENTS.md

## Claude Code

- `.claude/rules/` 의 경로별 요약은 해당 경로의 파일을 읽거나 고칠 때 자동으로 들어온다. 요약은 짧으니 거기 적힌 규칙 파일을 연다.
- 의미 규칙 리뷰를 요청받으면 `arch-review` 스킬(`.claude/skills/arch-review/SKILL.md`)을 따른다.
- 커밋·PR 메시지는 사용자가 요청할 때만 만든다.
