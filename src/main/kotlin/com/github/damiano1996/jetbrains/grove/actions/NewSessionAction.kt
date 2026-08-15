package com.github.damiano1996.jetbrains.grove.actions

import com.github.damiano1996.jetbrains.grove.model.SessionStatus
import com.github.damiano1996.jetbrains.grove.model.WorktreeSession
import com.github.damiano1996.jetbrains.grove.service.GitWorktreeService
import com.github.damiano1996.jetbrains.grove.service.ProjectOpener
import com.github.damiano1996.jetbrains.grove.service.SessionRegistryService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File
import java.util.UUID

class NewSessionAction : AnAction("New Grove Session...") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        createSession(project)
    }

    companion object {
        /** Shared by the menu/keyboard action and the tool window's "+" toolbar button. */
        fun createSession(project: Project) {
            val projectBasePath = project.basePath ?: return
            val currentDir = File(projectBasePath)

            // Run git commands from wherever we are (works from any worktree of the repo), but
            // anchor the new worktree's location on the *main* repo, not the worktree the user
            // happens to be sitting in right now - otherwise sessions created from inside a
            // worktree window nest under that worktree instead of sitting beside it.
            val repoRoot = GitWorktreeService.repoRootOf(currentDir)
            val mainRepoRoot = GitWorktreeService.mainRepoRootOf(currentDir)
            if (repoRoot == null || mainRepoRoot == null) {
                Messages.showErrorDialog(project, "Not inside a git repository.", "Grove")
                return
            }

            if (!GitWorktreeService.hasCommits(mainRepoRoot)) {
                val proceed =
                    Messages.showYesNoDialog(
                        project,
                        "This repository has no commits yet. Grove will create an empty initial " +
                            "commit so a worktree can be created - but any files you haven't " +
                            "committed in the main window won't appear in the new one. Continue?",
                        "Grove",
                        Messages.getWarningIcon(),
                    )
                if (proceed != Messages.YES) return
            }

            val branch =
                Messages.showInputDialog(
                    project,
                    "Branch name for the new worktree session:",
                    "New Grove Session",
                    null,
                )?.trim()

            if (branch.isNullOrEmpty()) return

            val worktreePath = mainRepoRoot.parentFile.resolve("${mainRepoRoot.name}-worktrees").resolve(branch)

            ProgressManager.getInstance().run(
                object : Task.Backgroundable(project, "Creating worktree for '$branch'", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val error = GitWorktreeService.addWorktree(repoRoot, worktreePath, branch)

                        if (error != null) {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog(
                                    project,
                                    "Failed to create worktree at ${worktreePath.absolutePath}:\n\n$error",
                                    "Grove",
                                )
                            }
                            return
                        }

                        val session =
                            WorktreeSession(
                                id = UUID.randomUUID().toString(),
                                branch = branch,
                                worktreePath = worktreePath.absolutePath,
                                mainRepoPath = mainRepoRoot.absolutePath,
                                status = SessionStatus.PENDING,
                            )
                        SessionRegistryService.getInstance().register(session)

                        ProjectOpener.openInNewWindow(worktreePath)
                    }
                },
            )
        }
    }
}
