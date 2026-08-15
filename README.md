<div align="center">

<img src="src/main/resources/META-INF/pluginIcon.svg" height="100" alt="Grove">

# Grove: Parallel Claude Code Sessions for JetBrains IDEs

[![latest tag badge]][latest tag link]
[![CI checks on main badge]][CI checks on main link]
[![latest commit to main badge]][latest commit to main link]
[![license badge]][license link]

</div>

<!-- Plugin description -->
**Grove** manages parallel Claude Code sessions, each bound to its own git worktree and its own IDE window.
Start a session from the Grove tool window, get a dedicated worktree, a dedicated IntelliJ window, and an
embedded terminal already running `claude` in it — with a single dashboard showing every session across every
open window, so you always know which window belongs to which branch and which session.
<!-- Plugin description end -->

---

## Why

Running multiple Claude Code sessions in parallel worktrees is easy from a terminal, but reviewing and editing
those changes in a JetBrains IDE means manually creating worktrees, opening new windows, and keeping track of
which window is which. Grove closes that gap: one action creates the worktree, opens the window, and launches
the session's terminal, and the tool window keeps a live map of session → branch → window across your whole IDE.

## Key Features

- **New session, one click.** Creates a git worktree, opens it in a new IntelliJ window, and auto-launches a
  `claude` terminal in it.
- **Live session dashboard.** The Grove tool window lists every session across every open window - branch,
  Claude's own AI-generated title for what it's working on, live status (`PENDING` / `ACTIVE` / `IDLE`), whether
  the window is currently `OPEN` or `CLOSED`, path, and created time.
- **Discovers worktrees Grove didn't create.** Worktrees added manually with `git worktree add`, or by an
  external `claude` session, show up too - scanned automatically on window open, or on demand via the Refresh
  action.
- **Session titles from claude itself.** Grove reads claude's own session transcript to mirror its AI-generated
  title into the table, so you can tell sessions apart without opening each window.
- **Clean removal.** Closes the session's window (if open) before removing the worktree, so IntelliJ doesn't
  write stray files back into the directory mid-delete.

## Status

Early development (0.x) - built and tested against IntelliJ IDEA Community 2025.2.

## Development

```bash
./gradlew runIde
```

## Contribution

Contributions are welcome! If you'd like to help improve Grove, follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and submit a pull request.
4. Open **issues** for suggestions, bug reports, or enhancements.

## License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more details.

[latest tag badge]: https://badgen.net/github/tag/damiano1996/grove-plugin?icon=github
[latest tag link]: https://github.com/damiano1996/grove-plugin/tags

[CI checks on main badge]: https://badgen.net/github/checks/damiano1996/grove-plugin/main?label=CI%20status%20on%20main&cache=900&icon=github
[CI checks on main link]: https://github.com/damiano1996/grove-plugin/actions

[latest commit to main badge]: https://badgen.net/github/last-commit/damiano1996/grove-plugin/main?icon=github
[latest commit to main link]: https://github.com/damiano1996/grove-plugin/commits/main

[license badge]: https://badgen.net/github/license/damiano1996/grove-plugin
[license link]: https://github.com/damiano1996/grove-plugin/blob/main/LICENSE
