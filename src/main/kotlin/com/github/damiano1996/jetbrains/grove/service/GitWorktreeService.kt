package com.github.damiano1996.jetbrains.grove.service

import com.intellij.openapi.diagnostic.logger
import java.io.File

data class GitWorktreeInfo(val path: String, val branch: String?, val head: String)

private data class GitResult(val success: Boolean, val stdout: String, val error: String)

/** Thin wrapper around the `git worktree` CLI. No git4idea dependency needed for this scope. */
object GitWorktreeService {

    private val log = logger<GitWorktreeService>()

    fun listWorktrees(repoRoot: File): List<GitWorktreeInfo> {
        val result = runGit(repoRoot, "worktree", "list", "--porcelain")
        if (!result.success) return emptyList()

        val worktrees = mutableListOf<GitWorktreeInfo>()
        var path: String? = null
        var head = ""
        var branch: String? = null

        fun flush() {
            path?.let { worktrees.add(GitWorktreeInfo(it, branch, head)) }
            path = null
            branch = null
            head = ""
        }

        result.stdout.lineSequence().forEach { line ->
            when {
                line.startsWith("worktree ") -> {
                    flush()
                    path = line.removePrefix("worktree ").trim()
                }

                line.startsWith("HEAD ") -> head = line.removePrefix("HEAD ").trim()
                line.startsWith("branch ") ->
                    branch = line.removePrefix("branch ").removePrefix("refs/heads/").trim()
            }
        }
        flush()

        return worktrees
    }

    /** True once `HEAD` resolves to a real commit (false for a freshly `git init`-ed repo). */
    fun hasCommits(repoRoot: File): Boolean = runGit(repoRoot, "rev-parse", "--verify", "--quiet", "HEAD").success

    /** Returns `null` on success, or a human-readable error (git's stderr) on failure. */
    fun addWorktree(repoRoot: File, worktreePath: File, branch: String, baseRef: String = "HEAD"): String? {
        val branchExists = runGit(repoRoot, "rev-parse", "--verify", "--quiet", "refs/heads/$branch").success

        // A freshly `git init`-ed repo with no commits yet has no HEAD to branch off of.
        if (!branchExists && !hasCommits(repoRoot)) {
            val emptyCommit = runGit(repoRoot, "commit", "--allow-empty", "-m", "Initial commit")
            if (!emptyCommit.success) {
                return "Repository has no commits yet, and creating an initial empty commit failed: ${emptyCommit.error}"
            }
        }

        val args =
            if (branchExists) {
                arrayOf("worktree", "add", worktreePath.absolutePath, branch)
            } else {
                arrayOf("worktree", "add", "-b", branch, worktreePath.absolutePath, baseRef)
            }

        val result = runGit(repoRoot, *args)
        return if (result.success) null else result.error.ifBlank { "git worktree add exited with an error." }
    }

    /** Returns `null` on success, or a human-readable error (git's stderr) on failure. */
    fun removeWorktree(repoRoot: File, worktreePath: File, force: Boolean = false): String? {
        val args = mutableListOf("worktree", "remove", worktreePath.absolutePath)
        if (force) args.add("--force")

        val result = runGit(repoRoot, *args.toTypedArray())
        return if (result.success) null else result.error.ifBlank { "git worktree remove exited with an error." }
    }

    /**
     * The top-level of *this* working tree - if [path] is inside a linked worktree, this
     * returns that worktree's own root, not the original repo. Use [mainRepoRootOf] to always
     * resolve back to the original repo regardless of which worktree you're currently in.
     */
    fun repoRootOf(path: File): File? {
        val result = runGit(path, "rev-parse", "--show-toplevel")
        if (!result.success) return null
        return File(result.stdout.trim())
    }

    /**
     * The main repository root, resolved via the shared `.git` directory - correct even when
     * [path] is inside a linked worktree (whereas `git rev-parse --show-toplevel` from a
     * worktree returns that worktree's own root instead).
     */
    fun mainRepoRootOf(path: File): File? {
        val result = runGit(path, "rev-parse", "--git-common-dir")
        if (!result.success) return null

        val raw = File(result.stdout.trim())
        val gitCommonDir = if (raw.isAbsolute) raw else File(path, raw.path)
        return gitCommonDir.canonicalFile.parentFile
    }

    private fun runGit(workingDir: File, vararg args: String): GitResult =
        try {
            val process =
                ProcessBuilder("git", *args)
                    .directory(workingDir)
                    .start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()

            if (exit != 0) {
                log.warn("git ${args.joinToString(" ")} failed (exit=$exit): $stderr")
                GitResult(success = false, stdout = stdout, error = stderr.trim())
            } else {
                GitResult(success = true, stdout = stdout, error = "")
            }
        } catch (e: Exception) {
            log.warn("git ${args.joinToString(" ")} failed", e)
            GitResult(success = false, stdout = "", error = e.message ?: "Unknown error running git")
        }
}
