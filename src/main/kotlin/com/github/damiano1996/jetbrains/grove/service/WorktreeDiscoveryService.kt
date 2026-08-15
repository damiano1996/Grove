package com.github.damiano1996.jetbrains.grove.service

import com.github.damiano1996.jetbrains.grove.model.SessionStatus
import com.github.damiano1996.jetbrains.grove.model.WorktreeSession
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.util.UUID

/**
 * Finds git worktrees that exist on disk but aren't tracked by Grove yet - created manually with
 * `git worktree add`, or by an external `claude` session - and registers them so they show up
 * in the tool window alongside worktrees Grove itself created.
 */
object WorktreeDiscoveryService {

    private val log = logger<WorktreeDiscoveryService>()

    fun scanFromOpenProject(project: Project) {
        val basePath = project.basePath ?: return
        val mainRepoRoot = GitWorktreeService.mainRepoRootOf(File(basePath)) ?: return
        scan(mainRepoRoot)
    }

    fun scan(mainRepoRoot: File) {
        try {
            val registry = SessionRegistryService.getInstance()

            for (info in GitWorktreeService.listWorktrees(mainRepoRoot)) {
                val path = File(info.path)
                if (path.absolutePath == mainRepoRoot.absolutePath) continue // the main repo itself
                if (registry.findByWorktreePath(path.absolutePath) != null) continue // already tracked

                val isOpen = ProjectManager.getInstance().openProjects.any { it.basePath == path.absolutePath }
                registry.register(
                    WorktreeSession(
                        id = UUID.randomUUID().toString(),
                        branch = info.branch ?: path.name,
                        worktreePath = path.absolutePath,
                        mainRepoPath = mainRepoRoot.absolutePath,
                        status = if (isOpen) SessionStatus.ACTIVE else SessionStatus.IDLE,
                        // May already have its own claude session running elsewhere (e.g. a plain
                        // terminal) - don't auto-launch a duplicate one when the window is opened.
                        terminalLaunched = true,
                    ),
                )
            }
        } catch (e: Exception) {
            log.warn("Grove worktree discovery scan failed", e)
        }
    }
}
