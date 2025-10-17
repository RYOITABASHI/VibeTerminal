package com.vibeterminal.ui.git

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Git workflow assistance
 */
class GitViewModel : ViewModel() {

    private val _gitStatus = MutableStateFlow(GitStatus())
    val gitStatus: StateFlow<GitStatus> = _gitStatus.asStateFlow()

    private val _suggestedActions = MutableStateFlow<List<GitAction>>(emptyList())
    val suggestedActions: StateFlow<List<GitAction>> = _suggestedActions.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var workingDirectory: File? = null

    fun initialize(workingDir: File) {
        workingDirectory = workingDir
        refreshStatus()
    }

    /**
     * Refresh Git status
     */
    fun refreshStatus() {
        viewModelScope.launch {
            _isRefreshing.value = true

            try {
                val status = fetchGitStatus()
                _gitStatus.value = status
                _suggestedActions.value = GitActionSuggestions.getSuggestedActions(status)
            } catch (e: Exception) {
                // Handle error
                _gitStatus.value = GitStatus(isRepository = false)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Fetch Git status by executing git commands
     */
    private suspend fun fetchGitStatus(): GitStatus {
        val workingDir = workingDirectory ?: return GitStatus(isRepository = false)

        // Check if it's a git repository
        if (!File(workingDir, ".git").exists()) {
            return GitStatus(isRepository = false)
        }

        var currentBranch: String? = null
        var modifiedFiles = 0
        var stagedFiles = 0
        var untrackedFiles = 0
        var aheadCommits = 0
        var behindCommits = 0
        var hasRemote = false
        var remoteName: String? = null
        var conflictFiles = 0

        try {
            // Get current branch
            currentBranch = executeGitCommand(workingDir, "git", "rev-parse", "--abbrev-ref", "HEAD")
                .trim()

            // Get remote info
            val remoteOutput = executeGitCommand(workingDir, "git", "remote")
            if (remoteOutput.isNotBlank()) {
                hasRemote = true
                remoteName = remoteOutput.lines().firstOrNull()?.trim()
            }

            // Get status --porcelain
            val statusOutput = executeGitCommand(workingDir, "git", "status", "--porcelain")
            statusOutput.lines().forEach { line ->
                if (line.isBlank()) return@forEach

                val status = line.substring(0, 2)
                when {
                    status[0] != ' ' && status[0] != '?' -> stagedFiles++
                    status[1] == 'M' || status[1] == 'D' -> modifiedFiles++
                    status[0] == '?' && status[1] == '?' -> untrackedFiles++
                    status.contains("U") -> conflictFiles++
                }
            }

            // Get ahead/behind info
            if (hasRemote && remoteName != null) {
                try {
                    val revListOutput = executeGitCommand(
                        workingDir,
                        "git", "rev-list", "--left-right", "--count",
                        "$remoteName/$currentBranch...$currentBranch"
                    )
                    val parts = revListOutput.trim().split("\\s+".toRegex())
                    if (parts.size == 2) {
                        behindCommits = parts[0].toIntOrNull() ?: 0
                        aheadCommits = parts[1].toIntOrNull() ?: 0
                    }
                } catch (e: Exception) {
                    // Remote branch might not exist yet
                }
            }

        } catch (e: Exception) {
            // Error executing git commands
        }

        return GitStatus(
            isRepository = true,
            currentBranch = currentBranch,
            modifiedFiles = modifiedFiles,
            stagedFiles = stagedFiles,
            untrackedFiles = untrackedFiles,
            aheadCommits = aheadCommits,
            behindCommits = behindCommits,
            hasRemote = hasRemote,
            remoteName = remoteName,
            conflictFiles = conflictFiles
        )
    }

    /**
     * Execute git command and return output
     */
    private suspend fun executeGitCommand(workingDir: File, vararg command: String): String {
        return try {
            val process = ProcessBuilder(*command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Execute a git action
     */
    fun executeAction(action: GitAction, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val workingDir = workingDirectory ?: run {
                onResult(false, "Working directory not set")
                return@launch
            }

            try {
                val commands = action.command.split(" ")
                val output = executeGitCommand(workingDir, *commands.toTypedArray())

                onResult(true, output)

                // Refresh status after action
                delay(500)
                refreshStatus()

            } catch (e: Exception) {
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }
}
