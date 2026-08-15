# Grove Changelog

## [Unreleased]

### Added

- Tool window with a live dashboard of Grove sessions: branch, Claude's own AI-generated title, status
  (`PENDING` / `ACTIVE` / `IDLE`), window open/closed state, path, and created time.
- One-click session creation: creates a git worktree, opens it in a new IntelliJ window, and auto-launches a
  `claude` terminal in it.
- Session title mirrored from claude's own transcript (`~/.claude/projects/.../*.jsonl`).
- Discovery of worktrees not created by Grove (manual `git worktree add`, or an external `claude` session),
  scanned on window open and via a manual Refresh action.
- Clean worktree removal: closes the session's window first to avoid IntelliJ writing stray files back into
  the directory mid-delete.
