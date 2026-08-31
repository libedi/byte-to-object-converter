---
name: git-worktree-create
description: 새 작업(feature/bugfix/실험)을 시작하기 전에 최신 base 브랜치(기본값 develop, 사용자 확인 후 결정)에서 분기한 격리된 git worktree와 브랜치를 생성한다. 사용자가 "worktree 만들어줘", "새 브랜치 파줘", "이 작업은 별도 디렉토리에서 하고 싶어", "지금 작업 디렉토리는 그대로 두고 새 기능 시작하자"처럼 "worktree"를 직접 언급하지 않아도 새로운 작업을 격리된 환경에서 시작하려 할 때 사용한다.
---

# Git Worktree 생성

새로운 작업이 **시작되기 전에** 격리된 worktree + 브랜치를 만들어서, 현재 작업 디렉토리를 건드리지 않게 한다. 네이밍은 나중에 diff를 보고 짓는 게 아니라, 사용자가 *미리* 설명한 작업 내용을 바탕으로 정한다.

## Gotchas

- base 브랜치명은 저장소마다 다를 수 있음(`main`, `develop`, `master` 등) — 기본 제안값 `develop`이 실제로는 존재하지 않는 저장소도 있으므로, 확인 없이 그대로 실행하지 않고 사용자가 답한 브랜치를 그대로 사용
- 로컬 `main`/현재 체크아웃된 브랜치는 오래됐을 수 있음 — 항상 `git fetch origin <base-branch>`로 최신화한 뒤 그 위에서 분기
- `.claude/worktrees/`가 대상 저장소의 `.gitignore`에 등록되어 있지 않으면 worktree 파일이 실수로 커밋될 수 있음 — user 레벨 skill이라 저장소마다 다를 수 있으니 생성 전에 `git check-ignore`로 확인할 것(이 프로젝트는 이미 등록되어 있지만 모든 저장소가 그런 건 아님)

## 언제 사용하는가

- 사용자가 새로운 작업(기능, 수정, 실험)을 시작하려 하고, 현재 작업 디렉토리와 분리하고 싶을 때
- 이미 커밋이 있는 기존 브랜치를 단순히 열람/리뷰하는 경우는 해당 없음 — 그럴 땐 네이밍 추론 없이 바로 `git worktree add <path> <existing-branch>` 실행

## 단계

1. **작업 내용 파악**: 사용자의 요청에 이미 무슨 작업을 할지 설명이 있으면 그것을 사용. "worktree 만들어줘"라고만 하고 맥락이 없으면, 진행 전에 어떤 작업인지 먼저 물어봄 — 브랜치명이 여기에 달려 있음

2. **base 브랜치 확인 (사용자 문의 필수) 및 동기화** (Gotchas 참고):
   - 기본값 `develop`을 제안하되 진행 전 반드시 사용자에게 확인 (예: "base 브랜치를 `origin/develop`에서 분기할까요? 다른 브랜치를 쓰시려면 알려주세요")
   - 사용자가 확인/지정한 브랜치를 이후 단계의 `<base-branch>`로 사용
   - `git fetch origin <base-branch>`로 최신화 후 그 위에서 분기

3. **브랜치명 추론**, 이 저장소의 기존 컨벤션(`feature/<slug>`, `feature/<이슈번호>-<slug>`, `bugfix/<slug>`)에 맞춤:
   - **접두사**: 결함/버그 수정이 명확하면 `bugfix/`, 그 외에는 기본값 `feature/`
   - **이슈번호**: 사용자가 실제로 이슈/티켓 번호를 언급한 경우에만 포함 — 임의로 지어내지 않음
   - **slug**: 짧은 kebab-case, 영문, 3~6단어로 작업 내용을 요약 (예: `feature/support-json-fields`)
   - worktree를 만들기 전에 제안하는 브랜치명을 한 줄로 먼저 알려줌 (질문이 아니라 진행하되, 어긋나면 바로잡을 수 있게)

4. **충돌 확인**: `git worktree list`와 `git branch --list <name>` 확인. 동일한 이름의 브랜치나 worktree 디렉토리가 이미 있으면 중단하고 재사용할지 다른 이름을 쓸지 사용자에게 확인 — 임의로 덮어쓰지 않음

5. **worktree 디렉토리 gitignore 확인** (Gotchas 참고): `git check-ignore -q .claude/worktrees` 실행(exit 0이면 이미 무시됨). 무시되지 않는다면 사용자에게 알리고, 동의하면 `.gitignore`에 `.claude/worktrees/` 한 줄을 추가한 뒤 진행 — 다른 저장소에도 이 skill이 쓰이므로 이 프로젝트처럼 이미 되어 있다고 단정하지 않음

6. **worktree 생성**:
   ```bash
   git worktree add -b <branch-name> .claude/worktrees/<dir-name> origin/<base-branch>
   ```
   - `<dir-name>`은 브랜치명의 `/`를 `-`로 바꾼 것 (예: `feature/12-add-x` → `.claude/worktrees/feature-12-add-x`)

7. **결과 보고**: 새 worktree의 절대 경로와 브랜치명을 보고해서 그 안에서 작업을 시작할 수 있게 함 (예: agent 세션을 몰아갈 경우 `EnterWorktree` 사용, 아니면 사용자에게 해당 경로로 `cd`하도록 안내)

## 가드레일

- base 브랜치는 반드시 사용자 확인 후 결정 — 기본값(`develop`)을 임의로 단정하고 진행하지 않음
- 로컬 브랜치에서 분기하지 않음 — 항상 새로 fetch한 `origin/<base-branch>` 기준
- 기존 worktree 디렉토리를 덮어쓰거나 기존 브랜치명을 재사용하기 전에 반드시 확인
- 언급되지 않은 이슈번호를 임의로 추론하지 않음
- 이 skill은 worktree 생성까지만 담당 — 실제 작업이나 이후 정리는 하지 않음. 제거(`git worktree remove <path>` + `git branch -d <branch>`)는 작업이 merge된 후 별도로 진행하는 것
