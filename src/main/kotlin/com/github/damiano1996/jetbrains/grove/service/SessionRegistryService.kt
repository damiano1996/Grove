package com.github.damiano1996.jetbrains.grove.service

import com.github.damiano1996.jetbrains.grove.model.SessionStatus
import com.github.damiano1996.jetbrains.grove.model.WorktreeSession
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil

fun interface SessionsChangedListener {
    fun sessionsChanged()
}

/**
 * Application-level (not project-level) so every open IntelliJ window - one per worktree - sees
 * the same list of sessions, since all windows share one IDE process and message bus.
 */
@Service(Service.Level.APP)
@State(
    name = "GroveSessionRegistry",
    storages = [Storage("grove-sessions.xml", roamingType = RoamingType.DISABLED)],
)
class SessionRegistryService : PersistentStateComponent<SessionRegistryService.State> {

    class State {
        var sessions: MutableList<WorktreeSession> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun all(): List<WorktreeSession> = myState.sessions.toList()

    fun findByWorktreePath(path: String): WorktreeSession? = myState.sessions.find { it.worktreePath == path }

    fun register(session: WorktreeSession) {
        myState.sessions.removeIf { it.id == session.id }
        myState.sessions.add(session)
        publish()
    }

    fun updateStatus(id: String, status: SessionStatus) {
        myState.sessions.find { it.id == id }?.let {
            it.status = status
            publish()
        }
    }

    fun markTerminalLaunched(id: String) {
        myState.sessions.find { it.id == id }?.let {
            it.terminalLaunched = true
            publish()
        }
    }

    fun updateClaudeTitle(id: String, title: String) {
        myState.sessions.find { it.id == id }?.let {
            if (it.claudeTitle == title) return
            it.claudeTitle = title
            publish()
        }
    }

    fun remove(id: String) {
        myState.sessions.removeIf { it.id == id }
        publish()
    }

    private fun publish() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).sessionsChanged()
    }

    companion object {
        val TOPIC: Topic<SessionsChangedListener> =
            Topic.create("GroveSessionsChanged", SessionsChangedListener::class.java)

        fun getInstance(): SessionRegistryService =
            ApplicationManager.getApplication().getService(SessionRegistryService::class.java)
    }
}
