package com.github.damiano1996.jetbrains.grove.service

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Exercises [GitWorktreeService] against a real, throwaway git repository - no IDE fixture needed. */
class GitWorktreeServiceTest {

    private lateinit var repoRoot: File

    @Before
    fun setUp() {
        repoRoot = Files.createTempDirectory("grove-test-repo").toFile()
        runGit(repoRoot, "init", "-b", "main")
        runGit(repoRoot, "config", "user.email", "test@example.com")
        runGit(repoRoot, "config", "user.name", "Test")
        runGit(repoRoot, "commit", "--allow-empty", "-m", "Initial commit")
    }

    @After
    fun tearDown() {
        repoRoot.deleteRecursively()
    }

    @Test
    fun `repoRootOf resolves the repository root`() {
        val nested = File(repoRoot, "sub").apply { mkdirs() }
        val resolved = GitWorktreeService.repoRootOf(nested)
        assertEquals(repoRoot.canonicalFile, resolved?.canonicalFile)
    }

    @Test
    fun `repoRootOf returns null outside a git repository`() {
        val notARepo = Files.createTempDirectory("grove-not-a-repo").toFile()
        try {
            assertNull(GitWorktreeService.repoRootOf(notARepo))
        } finally {
            notARepo.deleteRecursively()
        }
    }

    @Test
    fun `addWorktree creates a new branch and worktree`() {
        val worktreePath = File(repoRoot.parentFile, "grove-test-worktree")
        try {
            val error = GitWorktreeService.addWorktree(repoRoot, worktreePath, "feature-x")
            assertNull(error)
            assertTrue(worktreePath.exists())

            val worktrees = GitWorktreeService.listWorktrees(repoRoot)
            val added = worktrees.find { File(it.path).canonicalFile == worktreePath.canonicalFile }
            assertNotNull(added)
            assertEquals("feature-x", added?.branch)
        } finally {
            worktreePath.deleteRecursively()
        }
    }

    @Test
    fun `removeWorktree removes a previously added worktree`() {
        val worktreePath = File(repoRoot.parentFile, "grove-test-worktree-remove")
        GitWorktreeService.addWorktree(repoRoot, worktreePath, "feature-y")

        val error = GitWorktreeService.removeWorktree(repoRoot, worktreePath, force = true)
        assertNull(error)
        assertTrue(GitWorktreeService.listWorktrees(repoRoot).none { File(it.path).canonicalFile == worktreePath.canonicalFile })
    }

    private fun runGit(dir: File, vararg args: String) {
        val process = ProcessBuilder("git", *args).directory(dir).start()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed" }
    }
}
