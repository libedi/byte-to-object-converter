---
name: git-commit-push
description: 현재 변경사항을 커밋하고, main 브랜치 위로 rebase한 뒤 push한다. 사용자가 "커밋하고 푸시해줘"라고 하거나, main 위로 rebase 후 push하길 원할 때 사용한다.
---

# Git 커밋 & Push (main 위로 rebase)

대기 중인 변경사항을 커밋하고, 최신 `main`으로 rebase한 다음 push한다.

## 단계

1. **현재 상태 파악** (병렬로 실행):
   - `git status` — staged/unstaged/untracked 파일 확인
   - `git diff` (및 `git diff --staged`) — 변경 내용 확인
   - `git log --oneline -10` — 이 저장소의 기존 커밋 메시지 스타일 파악
   - `git branch --show-current` — 현재 `main` 브랜치가 아닌지 확인. `main`이면 중단하고 사용자에게 알림 (`main`에서 직접 커밋/rebase/push 금지)

2. **staging 및 커밋**:
   - 관련 파일을 이름으로 명시해서 stage (`git add -A`, `git add .` 금지) — `git status`를 검토해서 비밀정보나 무관한 파일이 섞이지 않게 함
   - 변경 성격이 서로 다르면(예: 소스 리팩토링 / 빌드 설정 / 신규 도구 파일) 이 저장소의 기존 방식대로 커밋을 나눠서 작성
   - 커밋 메시지는 "무엇을"보다 "왜"에 초점을 맞추고, 이 저장소의 기존 스타일에 맞춰 heredoc으로 작성:
     ```bash
     git commit -m "$(cat <<'EOF'
     <요약 한 줄>

     Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
     EOF
     )"
     ```
   - 커밋할 변경사항이 없다면(clean tree) 커밋은 건너뛰고, 현재 브랜치가 원격 추적 브랜치보다 앞서 있는 경우에만 rebase/push로 진행

3. **main 최신화 및 rebase**:
   - `git fetch origin main`
   - `git rebase origin/main`
   - 충돌 발생 시: 중단하고 충돌 파일을 사용자에게 보여준 뒤 직접 해결하게 하거나 어떻게 진행할지 물어봄. 사용자 지시 없이 `git rebase --abort`를 실행하거나 충돌을 임의로 밀어붙이지 않음

4. **push 전 PR 상태 확인**:
   - `gh pr view --json url,number,state 2>/dev/null`로 현재 브랜치에 연결된 PR 확인
   - `gh pr view`는 상태와 무관하게(MERGED/CLOSED여도) 그 브랜치에 연결됐던 PR을 찾아서 보여준다는 점에 주의 — **state를 직접 확인해야 함**
   - PR이 없으면 정상 진행 (신규 브랜치)
   - PR이 있고 `state == "OPEN"`이면 정상 진행
   - PR이 있고 `state`가 `MERGED` 또는 `CLOSED`이면 **push하지 말고 중단**. 이미 병합/종료된 PR의 브랜치에 새 커밋을 쌓고 있다는 뜻이므로(브랜치를 잘못 이어서 작업 중일 가능성), 사용자에게 상황을 알리고 어떻게 할지 확인:
     - 계속 이 브랜치에 push하고 새 PR을 만들지 (예: 후속 작업으로 의도된 경우)
     - 아니면 `git-worktree-create`로 최신 main에서 새 브랜치를 파서 그쪽으로 옮길지

5. **push** (4번에서 진행 가능 확인된 경우만):
   - 먼저 일반 push 시도: `git push`
   - non-fast-forward로 거부되고(rebase로 커밋이 재배치되어 일어날 수 있는 정상적인 상황) 원격 추적 브랜치가 있다면 `git push --force-with-lease`로 push. 이 방식은 마지막으로 fetch한 상태와 원격이 여전히 일치할 때만 덮어쓰기 때문에 안전함 — 그 사이 원격이 변경됐다면 덮어쓰는 대신 실패함
   - `--force-with-lease`마저 거부되면(그 사이 다른 사람이 이 브랜치에 push함) 일반 `--force`로 재시도하지 말고 중단해서 사용자에게 보고
   - 원격 추적 브랜치가 아직 없다면 `git push -u origin <branch>`로 push

6. **PR 페이지 열람/생성**:
   - 4번에서 이미 확인한 PR 상태를 재사용: `state == "OPEN"`인 PR이 있으면 `gh pr view --web`으로 열기. 브라우저가 안 열리는 환경일 수 있으니 URL도 텍스트로 함께 보고
   - OPEN PR이 없으면(신규 브랜치이거나, 4번에서 사용자가 "새 PR 생성"을 택한 경우) **사용자에게 먼저 생성 여부를 확인** (PR 생성은 외부에 노출되는 행위이므로 임의로 만들지 않음). 승인하면 이 브랜치의 커밋(들)을 바탕으로 제목/설명(짧은 제목 + `## Summary` + `## Test plan`)을 작성해 `gh pr create` 실행 후 `gh pr view --web`으로 열고 URL 보고

7. **결과 보고**: 무엇을 커밋했는지, rebase가 일어났는지(재배치된 커밋 수 포함), push가 성공했는지, PR URL까지 포함해 요약 — 브랜치명도 함께 명시

## 가드레일

- `main`/`master`에서 직접 실행 금지
- 일반 `git push --force` 금지 — 오직 `--force-with-lease`만 사용
- hook 건너뛰기(`--no-verify`)나 GPG 서명 우회 금지
- `git rebase`에서 충돌이 나면 임의로 해결하지 말고 사용자에게 확인
- 이 흐름에서 기존 커밋을 amend하지 않음 — 대기 중인 변경사항은 항상 새 커밋으로 생성
- 사용자 확인 없이 PR을 생성하지 않음 — 기존 OPEN PR을 열람만 하는 건 확인 불필요
- 브랜치에 연결된 PR이 MERGED/CLOSED 상태면 사용자 확인 없이 push하지 않음
