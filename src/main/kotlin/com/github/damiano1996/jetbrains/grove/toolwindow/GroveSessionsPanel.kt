package com.github.damiano1996.jetbrains.grove.toolwindow

import com.github.damiano1996.jetbrains.grove.actions.NewSessionAction
import com.github.damiano1996.jetbrains.grove.model.WorktreeSession
import com.github.damiano1996.jetbrains.grove.service.ClaudeTerminalLauncher
import com.github.damiano1996.jetbrains.grove.service.GitWorktreeService
import com.github.damiano1996.jetbrains.grove.service.ProjectOpener
import com.github.damiano1996.jetbrains.grove.service.SessionRegistryService
import com.github.damiano1996.jetbrains.grove.service.SessionsChangedListener
import com.github.damiano1996.jetbrains.grove.service.WorktreeDiscoveryService
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss")

private fun openProjectFor(session: WorktreeSession): Project? =
    ProjectManager.getInstance().openProjects.find { it.basePath == session.worktreePath }

private class SessionTableModel : AbstractTableModel() {
    private val columns = arrayOf("Branch", "Title", "Status", "Window", "Path", "Created")
    var sessions: List<WorktreeSession> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    override fun getRowCount() = sessions.size

    override fun getColumnCount() = columns.size

    override fun getColumnName(column: Int) = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val s = sessions[rowIndex]
        return when (columnIndex) {
            0 -> s.branch
            1 -> s.claudeTitle ?: ""
            2 -> s.status.name
            3 -> if (openProjectFor(s) != null) "OPEN" else "CLOSED"
            4 -> s.worktreePath
            5 -> TIME_FORMAT.format(Date(s.createdAt))
            else -> ""
        }
    }
}

class GroveSessionsPanel(private val currentProject: Project) : JPanel(BorderLayout()) {

    private val tableModel = SessionTableModel()
    private val table =
        JBTable(tableModel).apply {
            setStriped(true)
            setShowGrid(false)
            emptyText.text = "No Grove sessions yet"
            emptyText.appendSecondaryText(
                "Create session",
                SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
            ) { NewSessionAction.createSession(currentProject) }
        }

    init {
        val decoratedPanel =
            ToolbarDecorator.createDecorator(table)
                .setAddAction { NewSessionAction.createSession(currentProject) }
                .setAddActionName("New Grove Session")
                .setRemoveAction { removeSelected() }
                .setRemoveActionName("Remove Worktree")
                .disableUpDownActions()
                .addExtraAction(OpenOrFocusAction())
                .addExtraAction(LaunchTerminalAction())
                .addExtraAction(RefreshAction())
                .createPanel()

        add(decoratedPanel, BorderLayout.CENTER)

        refresh()

        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(SessionRegistryService.TOPIC, SessionsChangedListener { refresh() })
    }

    private fun selected(): WorktreeSession? {
        val row = table.selectedRow
        if (row < 0) return null
        return tableModel.sessions[row]
    }

    private fun removeSelected() {
        val session = selected() ?: return
        val confirm =
            Messages.showYesNoDialog(
                currentProject,
                "Remove worktree at ${session.worktreePath}? This deletes the checkout, not the branch.",
                "Remove Grove Session",
                Messages.getWarningIcon(),
            )
        if (confirm != Messages.YES) return

        // Close the window first: if it's still open, IntelliJ can autosave (.idea files, etc.)
        // back into the directory while/after git deletes it, leaving stray files that make the
        // path non-empty and block re-creating a worktree with the same name later.
        openProjectFor(session)?.let { ProjectManager.getInstance().closeAndDispose(it) }

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(currentProject, "Removing worktree '${session.branch}'", false) {
                override fun run(indicator: ProgressIndicator) {
                    val error = GitWorktreeService.removeWorktree(File(session.mainRepoPath), File(session.worktreePath), force = true)

                    val worktreeDir = File(session.worktreePath)
                    if (worktreeDir.exists()) {
                        worktreeDir.deleteRecursively()
                    }

                    if (error != null && worktreeDir.exists()) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                currentProject,
                                "Failed to fully remove worktree at ${session.worktreePath}:\n\n$error",
                                "Grove",
                            )
                        }
                        return
                    }

                    SessionRegistryService.getInstance().remove(session.id)
                }
            },
        )
    }

    private fun refresh() {
        SwingUtilities.invokeLater {
            val selectedId = selected()?.id
            tableModel.sessions = SessionRegistryService.getInstance().all()
            selectedId?.let { id ->
                val row = tableModel.sessions.indexOfFirst { it.id == id }
                if (row >= 0) table.setRowSelectionInterval(row, row)
            }
        }
    }

    private inner class OpenOrFocusAction :
        AnAction("Open / Focus Window", "Open this session's window, or focus it if already open", AllIcons.General.OpenInToolWindow) {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selected() != null
        }

        override fun actionPerformed(e: AnActionEvent) {
            val session = selected() ?: return
            val existing = openProjectFor(session)
            if (existing != null) {
                ProjectUtil.focusProjectWindow(existing, true)
            } else {
                ProjectOpener.openInNewWindow(File(session.worktreePath))
            }
        }
    }

    private inner class LaunchTerminalAction :
        AnAction("Launch Claude Terminal", "Open a claude terminal tab in this session's window", AllIcons.Debugger.Console) {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selected() != null
        }

        override fun actionPerformed(e: AnActionEvent) {
            val session = selected() ?: return
            val project = openProjectFor(session)
            if (project == null) {
                Messages.showWarningDialog(currentProject, "Open the window for this session first.", "Grove")
                return
            }
            ClaudeTerminalLauncher.launch(project, File(session.worktreePath), tabName = "Grove: ${session.branch}")
        }
    }

    private inner class RefreshAction :
        AnAction("Refresh", "Scan for worktrees not yet tracked by Grove (created manually or by an external claude session)", AllIcons.Actions.Refresh) {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            WorktreeDiscoveryService.scanFromOpenProject(currentProject)
        }
    }
}
