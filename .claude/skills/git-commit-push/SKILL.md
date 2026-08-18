---
name: git-commit-push
description: Commit the current changes, rebase the branch onto main, and push. Use when the user asks to commit and push, or wants their branch rebased onto main before pushing.
---

# Git Commit & Push (rebase onto main)

Commits pending changes, rebases the current branch onto the latest `main`, then pushes.

## Steps

1. **Inspect state** (run in parallel):
   - `git status` — see staged/unstaged/untracked files
   - `git diff` (and `git diff --staged`) — see what changed
   - `git log --oneline -10` — match this repo's commit message style
   - `git branch --show-current` — confirm we're not on `main` itself; if we are, stop and tell the user (never commit/rebase/push directly on `main`)

2. **Stage and commit**:
   - Stage relevant files explicitly by name (never `git add -A` / `git add .`) — review `git status` to avoid staging secrets or unrelated files.
   - Write a concise commit message (why over what), matching the repo's existing style, passed via heredoc:
     ```bash
     git commit -m "$(cat <<'EOF'
     <summary line>

     Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
     EOF
     )"
     ```
   - If there is nothing to commit (clean tree), skip committing and proceed to rebase/push only if the branch is already ahead of its remote tracking branch.

3. **Update main and rebase**:
   - `git fetch origin main`
   - `git rebase origin/main`
   - If conflicts occur: stop, show the conflicting files to the user, and let them resolve (or ask how to proceed). Do **not** run `git rebase --abort` or force through conflicts without the user's direction.

4. **Push**:
   - Try a normal push first: `git push`
   - If it's rejected as non-fast-forward (expected after a rebase that moved commits) and there is an upstream tracking branch, push with `git push --force-with-lease`. This is safe by design — it only overwrites the remote branch if it still matches what we last fetched, so it fails instead of clobbering someone else's work if the remote changed since.
   - If `--force-with-lease` itself is rejected (someone else pushed to this branch in the meantime), stop and report this to the user rather than retrying with plain `--force`.
   - If there's no upstream tracking branch yet, push with `git push -u origin <branch>`.

5. **Load the PR page**:
   - Check whether a PR already exists for this branch: `gh pr view --json url,number 2>/dev/null`.
   - If one exists, open it: `gh pr view --web`. Also report the PR URL in text in case the browser doesn't open in this environment.
   - If none exists, **ask the user** whether to create one (opening a PR is a visible, shared-state action) — don't create it silently. If they confirm, draft a title/summary from the commit(s) on this branch (per the PR-creation convention: short title, `## Summary` + `## Test plan` body), run `gh pr create`, then open it with `gh pr view --web` and report the URL.

6. **Report**: summarize what was committed, whether a rebase happened (and how many commits it replayed), that the push succeeded, and the PR URL — include the branch name.

## Guardrails

- Never run on `main`/`master` directly.
- Never use plain `git push --force` — only `--force-with-lease`.
- Never skip hooks (`--no-verify`) or bypass GPG signing.
- If `git rebase` reports conflicts, pause for the user — don't guess at resolutions.
- Don't amend existing commits as part of this flow; always create a new commit for pending changes.
- Don't create a PR without the user's confirmation; opening/viewing an existing PR needs no confirmation.
