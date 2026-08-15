package com.github.damiano1996.jetbrains.grove.service

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runBlockingCancellable
import java.io.File

/** Bridges the suspend `ProjectUtil.openOrImportAsync` API to plain callers (button handlers, actions). */
object ProjectOpener {
    fun openInNewWindow(path: File) {
        ApplicationManager.getApplication().executeOnPooledThread {
            runBlockingCancellable {
                ProjectUtil.openOrImportAsync(path.toPath(), OpenProjectTask { forceOpenInNewFrame = true })
            }
        }
    }
}
