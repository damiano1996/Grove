# Grove Changelog

## [Unreleased]

## [0.1.1]

### Fixed

- Critical compatibility break on IntelliJ 2025.3+/2026.x: `OpenProjectTask`'s internal constructor
  changed signature between platform versions, causing `NoSuchMethodError` on newer IDEs. Replaced
  with the long-stable `ProjectUtil.openOrImport(String, Project?, Boolean)` overload.
- Reverted `ClaudeTerminalLauncher` to the same non-internal `createNewSession(runner, tabState)`
  overload InCoder already uses in production, instead of an internal-marked overload that was only
  needed for a terminal-title-tracking approach since replaced by transcript-based title reading.

## [0.1.0]

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
