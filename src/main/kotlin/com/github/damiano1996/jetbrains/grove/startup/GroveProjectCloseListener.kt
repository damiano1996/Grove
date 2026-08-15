package com.github.damiano1996.jetbrains.grove.startup

import com.github.damiano1996.jetbrains.grove.model.SessionStatus
import com.github.damiano1996.jetbrains.grove.service.SessionRegistryService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

/** The other half of the Status column's liveness - see [GroveProjectActivity]. */
class GroveProjectCloseListener : ProjectManagerListener {

    override fun projectClosed(project: Project) {
        val basePath = project.basePath ?: return
        val registry = SessionRegistryService.getInstance()
        val session = registry.findByWorktreePath(basePath) ?: return
        registry.updateStatus(session.id, SessionStatus.IDLE)
    }
}
