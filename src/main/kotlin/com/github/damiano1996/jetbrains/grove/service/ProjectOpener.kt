package com.github.damiano1996.jetbrains.grove.service

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import java.io.File

object ProjectOpener {
    fun openInNewWindow(path: File) {
        ApplicationManager.getApplication().invokeLater {
            ProjectUtil.openOrImport(path.absolutePath, null, true)
        }
    }
}
