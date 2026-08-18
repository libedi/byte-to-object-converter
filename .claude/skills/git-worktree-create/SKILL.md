---
name: git-worktree-create
description: 새 작업(feature/bugfix)을 위해 최신 main에서 분기한 git worktree와 브랜치를 생성한다. 작업이 끝난 뒤가 아니라, 사용자가 앞으로 할 작업을 설명하며 격리된 worktree를 원할 때 작업 시작 전에 사용한다.
---

# Git Worktree 생성

새로운 작업이 **시작되기 전에** 격리된 worktree + 브랜치를 만들어서, 현재 작업 디렉토리를 건드리지 않게 한다. 네이밍은 나중에 diff를 보고 짓는 게 아니라, 사용자가 *미리* 설명한 작업 내용을 바탕으로 정한다.

## 언제 사용하는가

- 사용자가 새로운 작업(기능, 수정, 실험)을 시작하려 하고, 현재 작업 디렉토리와 분리하고 싶을 때
- 이미 커밋이 있는 기존 브랜치를 단순히 열람/리뷰하는 경우는 해당 없음 — 그럴 땐 네이밍 추론 없이 바로 `git worktree add <path> <existing-branch>` 실행

## 단계

1. **작업 내용 파악**: 사용자의 요청에 이미 무슨 작업을 할지 설명이 있으면 그것을 사용. "worktree 만들어줘"라고만 하고 맥락이 없으면, 진행 전에 어떤 작업인지 먼저 물어봄 — 브랜치명이 여기에 달려 있음

2. **base 브랜치 동기화**: `git fetch origin main` — 항상 최신 `origin/main` 기준으로 분기 (오래됐을 수 있는 로컬 `main`이나 현재 브랜치 기준 아님)

3. **브랜치명 추론**, 이 저장소의 기존 컨벤션(`feature/<slug>`, `feature/<이슈번호>-<slug>`, `bugfix/<slug>`)에 맞춤:
   - **접두사**: 결함/버그 수정이 명확하면 `bugfix/`, 그 외에는 기본값 `feature/`
   - **이슈번호**: 사용자가 실제로 이슈/티켓 번호를 언급한 경우에만 포함 — 임의로 지어내지 않음
   - **slug**: 짧은 kebab-case, 영문, 3~6단어로 작업 내용을 요약 (예: `feature/support-json-fields`)
   - worktree를 만들기 전에 제안하는 브랜치명을 한 줄로 먼저 알려줌 (질문이 아니라 진행하되, 어긋나면 바로잡을 수 있게)

4. **충돌 확인**: `git worktree list`와 `git branch --list <name>` 확인. 동일한 이름의 브랜치나 worktree 디렉토리가 이미 있으면 중단하고 재사용할지 다른 이름을 쓸지 사용자에게 확인 — 임의로 덮어쓰지 않음

5. **worktree 생성**:
   ```bash
   git worktree add -b <branch-name> .claude/worktrees/<dir-name> origin/main
   ```
   - `<dir-name>`은 브랜치명의 `/`를 `-`로 바꾼 것 (예: `feature/12-add-x` → `.claude/worktrees/feature-12-add-x`)
   - `.claude/worktrees/`는 이미 gitignore 처리되어 있어 커밋될 위험 없음

6. **결과 보고**: 새 worktree의 절대 경로와 브랜치명을 보고해서 그 안에서 작업을 시작할 수 있게 함 (예: agent 세션을 몰아갈 경우 `EnterWorktree` 사용, 아니면 사용자에게 해당 경로로 `cd`하도록 안내)

## 가드레일

- 로컬 `main`에서 분기하지 않음 — 항상 새로 fetch한 `origin/main` 기준
- 기존 worktree 디렉토리를 덮어쓰거나 기존 브랜치명을 재사용하기 전에 반드시 확인
- 언급되지 않은 이슈번호를 임의로 추론하지 않음
- 이 skill은 worktree 생성까지만 담당 — 실제 작업이나 이후 정리는 하지 않음. 제거(`git worktree remove <path>` + `git branch -d <branch>`)는 작업이 merge된 후 별도로 진행하는 것
