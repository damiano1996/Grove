package com.github.damiano1996.jetbrains.grove.model

enum class SessionStatus {
    /** Worktree created, window not opened yet. */
    PENDING,

    /** The window is currently open. */
    ACTIVE,

    /** The window was open at some point and has since been closed. */
    IDLE,
}

data class WorktreeSession(
    var id: String = "",
    var branch: String = "",
    var worktreePath: String = "",
    var mainRepoPath: String = "",
    var status: SessionStatus = SessionStatus.PENDING,
    var createdAt: Long = System.currentTimeMillis(),
    /** Whether the `claude` terminal has ever been auto-launched for this session - separate
     *  from [status] so reopening a closed window doesn't spawn a duplicate terminal tab. */
    var terminalLaunched: Boolean = false,
    /** Latest AI-generated summary claude set as its terminal title, if any. */
    var claudeTitle: String? = null,
)
