package com.github.damiano1996.jetbrains.grove.service

import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Polls each session's `claude` transcript - the `.jsonl` file under
 * `~/.claude/projects/<mangled-worktree-path>/` - for its latest
 * `{"type":"ai-title","aiTitle":"..."}` record and mirrors it into [SessionRegistryService].
 * This sidesteps the terminal's OSC-title plumbing entirely - see [ClaudeTerminalLauncher],
 * which attached a `TerminalTitleListener` that never fired on this platform build even for a
 * genuinely running `claude` process.
 */
object ClaudeTitleWatcher {

    private val log = logger<ClaudeTitleWatcher>()
    private val started = AtomicBoolean(false)
    private val AI_TITLE_REGEX = Regex(""""type":"ai-title","aiTitle":"((?:[^"\\]|\\.)*)"""")

    fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return

        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            { poll() },
            3,
            3,
            TimeUnit.SECONDS,
        )
    }

    private fun poll() {
        try {
            val registry = SessionRegistryService.getInstance()
            for (session in registry.all()) {
                val title = latestAiTitle(session.worktreePath) ?: continue
                if (title != session.claudeTitle) {
                    registry.updateClaudeTitle(session.id, title)
                }
            }
        } catch (e: Exception) {
            log.warn("Grove title poll failed", e)
        }
    }

    private fun latestAiTitle(worktreePath: String): String? {
        val dir = transcriptDir(worktreePath)
        val transcript = dir.listFiles { f -> f.extension == "jsonl" }?.maxByOrNull { it.lastModified() } ?: return null

        val text = transcript.readText()
        val match = AI_TITLE_REGEX.findAll(text).lastOrNull() ?: return null
        return unescapeJson(match.groupValues[1]).trim().ifBlank { null }
    }

    private fun transcriptDir(worktreePath: String): File {
        val home = System.getProperty("user.home")
        val mangled = "-" + worktreePath.removePrefix("/").replace('/', '-')
        return File(home, ".claude/projects/$mangled")
    }

    private fun unescapeJson(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i += 1
            }
        }
        return sb.toString()
    }
}
