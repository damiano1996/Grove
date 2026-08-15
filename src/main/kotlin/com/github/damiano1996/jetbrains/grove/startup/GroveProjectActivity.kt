package com.github.damiano1996.jetbrains.grove.startup

import com.github.damiano1996.jetbrains.grove.model.SessionStatus
import com.github.damiano1996.jetbrains.grove.service.ClaudeTerminalLauncher
import com.github.damiano1996.jetbrains.grove.service.ClaudeTitleWatcher
import com.github.damiano1996.jetbrains.grove.service.SessionRegistryService
import com.github.damiano1996.jetbrains.grove.service.WorktreeDiscoveryService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.io.File

/**
 * Runs on every project open. Scans for worktrees not yet tracked by Grove (manually created, or
 * by an external `claude` session - see [WorktreeDiscoveryService]), then, if the opened
 * project's path matches a Grove session, marks it ACTIVE (this is what keeps the tool window's
 * Status column live across window open/close - see [GroveProjectCloseListener] for the other
 * half) and, the first time only, auto-launches the `claude` terminal in it.
 */
class GroveProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        ClaudeTitleWatcher.ensureStarted()
        WorktreeDiscoveryService.scanFromOpenProject(project)

        val basePath = project.basePath ?: return
        val registry = SessionRegistryService.getInstance()
        val session = registry.findByWorktreePath(basePath) ?: return

        registry.updateStatus(session.id, SessionStatus.ACTIVE)

        if (!session.terminalLaunched) {
            ClaudeTerminalLauncher.launch(project, File(basePath), tabName = "Grove: ${session.branch}")
            registry.markTerminalLaunched(session.id)
        }
    }
}
