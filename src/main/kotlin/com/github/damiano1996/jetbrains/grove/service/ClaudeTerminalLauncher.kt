package com.github.damiano1996.jetbrains.grove.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.File

/** Launches `claude` in a dedicated terminal tab, mirroring InCoder's CommandLineTool pattern. */
object ClaudeTerminalLauncher {

    private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"

    fun launch(project: Project, workingDirectory: File, tabName: String) {
        ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow(TERMINAL_TOOL_WINDOW_ID)?.show()

            val tabState = TerminalTabState()
            tabState.myWorkingDirectory = workingDirectory.absolutePath
            tabState.myShellCommand = listOf("claude")
            tabState.myTabName = tabName
            tabState.myIsUserDefinedTabTitle = true

            TerminalToolWindowManager.getInstance(project).createNewSession(LocalTerminalDirectRunner(project), tabState)
        }
    }
}
