package com.vibeterminal.ui.git

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Git repository status
 */
data class GitStatus(
    val isRepository: Boolean = false,
    val currentBranch: String? = null,
    val modifiedFiles: Int = 0,
    val stagedFiles: Int = 0,
    val untrackedFiles: Int = 0,
    val aheadCommits: Int = 0,
    val behindCommits: Int = 0,
    val hasRemote: Boolean = false,
    val remoteName: String? = null,
    val conflictFiles: Int = 0
)

/**
 * Suggested Git action for beginners
 */
data class GitAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val command: String,
    val explanation: String,
    val requiresConfirmation: Boolean = false,
    val isDestructive: Boolean = false
)

/**
 * Git workflow state for beginners
 */
enum class GitWorkflowState {
    NO_REPO,           // Not a git repository
    CLEAN,             // No changes
    HAS_CHANGES,       // Unstaged changes
    HAS_STAGED,        // Staged changes, ready to commit
    AHEAD_OF_REMOTE,   // Has unpushed commits
    BEHIND_REMOTE,     // Need to pull
    DIVERGED,          // Need to merge/rebase
    CONFLICT           // Merge conflict
}

/**
 * Git action suggestions based on current state
 */
object GitActionSuggestions {

    fun getSuggestedActions(status: GitStatus): List<GitAction> {
        val actions = mutableListOf<GitAction>()
        val state = determineWorkflowState(status)

        when (state) {
            GitWorkflowState.NO_REPO -> {
                actions.add(
                    GitAction(
                        id = "init",
                        title = "Gitリポジトリを作成",
                        description = "このフォルダをGitで管理できるようにします",
                        icon = Icons.Default.CreateNewFolder,
                        command = "git init",
                        explanation = "git initは新しいGitリポジトリを作成します。\nファイルの変更履歴を追跡できるようになります。"
                    )
                )
            }

            GitWorkflowState.CLEAN -> {
                actions.add(
                    GitAction(
                        id = "status",
                        title = "状態を確認",
                        description = "現在のGit状態を表示します",
                        icon = Icons.Default.Info,
                        command = "git status",
                        explanation = "変更されたファイルや次に何をすべきか確認できます。"
                    )
                )
            }

            GitWorkflowState.HAS_CHANGES -> {
                actions.add(
                    GitAction(
                        id = "add_all",
                        title = "すべての変更をステージング",
                        description = "変更した${status.modifiedFiles}個のファイルをコミット対象に追加",
                        icon = Icons.Default.Add,
                        command = "git add .",
                        explanation = "git add . は変更したファイルを「コミット待ち」状態にします。\n次のステップはコミット作成です。"
                    )
                )
                actions.add(
                    GitAction(
                        id = "diff",
                        title = "変更内容を確認",
                        description = "何が変更されたか詳しく見る",
                        icon = Icons.Default.Compare,
                        command = "git diff",
                        explanation = "ファイルのどこが変更されたか、行単位で表示します。"
                    )
                )
            }

            GitWorkflowState.HAS_STAGED -> {
                actions.add(
                    GitAction(
                        id = "commit",
                        title = "コミットを作成",
                        description = "${status.stagedFiles}個のファイルをコミット",
                        icon = Icons.Default.Save,
                        command = "git commit",
                        explanation = "変更をセーブポイントとして記録します。\nメッセージで何を変更したか説明してください。",
                        requiresConfirmation = true
                    )
                )
                actions.add(
                    GitAction(
                        id = "unstage",
                        title = "ステージングを取り消し",
                        description = "コミット対象から外す",
                        icon = Icons.Default.Undo,
                        command = "git restore --staged .",
                        explanation = "ステージングを取り消します。\nファイルの変更自体は残ります。"
                    )
                )
            }

            GitWorkflowState.AHEAD_OF_REMOTE -> {
                if (status.hasRemote) {
                    actions.add(
                        GitAction(
                            id = "push",
                            title = "リモートにプッシュ",
                            description = "${status.aheadCommits}個のコミットをアップロード",
                            icon = Icons.Default.CloudUpload,
                            command = "git push",
                            explanation = "ローカルのコミットをGitHub等のリモートサーバーに送信します。",
                            requiresConfirmation = true
                        )
                    )
                } else {
                    actions.add(
                        GitAction(
                            id = "add_remote",
                            title = "リモートリポジトリを追加",
                            description = "GitHubなどのリモートを設定",
                            icon = Icons.Default.CloudQueue,
                            command = "git remote add origin <URL>",
                            explanation = "リモートリポジトリのURLを設定します。\nGitHubでリポジトリを作成後、URLをコピーして使います。",
                            requiresConfirmation = true
                        )
                    )
                }
            }

            GitWorkflowState.BEHIND_REMOTE -> {
                actions.add(
                    GitAction(
                        id = "pull",
                        title = "最新版を取得（Pull）",
                        description = "${status.behindCommits}個の新しいコミットがあります",
                        icon = Icons.Default.CloudDownload,
                        command = "git pull",
                        explanation = "リモートの最新変更をダウンロードして、ローカルに統合します。",
                        requiresConfirmation = true
                    )
                )
            }

            GitWorkflowState.DIVERGED -> {
                actions.add(
                    GitAction(
                        id = "pull_rebase",
                        title = "最新版を取得して統合",
                        description = "リモートとローカルの変更を統合",
                        icon = Icons.Default.MergeType,
                        command = "git pull --rebase",
                        explanation = "リモートとローカルの両方に変更がある場合、\n統合（rebase）して一つにまとめます。",
                        requiresConfirmation = true
                    )
                )
            }

            GitWorkflowState.CONFLICT -> {
                actions.add(
                    GitAction(
                        id = "show_conflicts",
                        title = "コンフリクトを確認",
                        description = "${status.conflictFiles}個のファイルで競合",
                        icon = Icons.Default.Warning,
                        command = "git status",
                        explanation = "どのファイルで競合が起きているか確認します。\n競合を手動で解決する必要があります。"
                    )
                )
            }
        }

        // Always available actions
        actions.add(
            GitAction(
                id = "log",
                title = "履歴を表示",
                description = "コミット履歴を見る",
                icon = Icons.Default.History,
                command = "git log --oneline -10",
                explanation = "最近のコミット履歴を表示します。"
            )
        )

        return actions
    }

    private fun determineWorkflowState(status: GitStatus): GitWorkflowState {
        if (!status.isRepository) return GitWorkflowState.NO_REPO

        if (status.conflictFiles > 0) return GitWorkflowState.CONFLICT

        if (status.aheadCommits > 0 && status.behindCommits > 0) {
            return GitWorkflowState.DIVERGED
        }

        if (status.behindCommits > 0) return GitWorkflowState.BEHIND_REMOTE

        if (status.aheadCommits > 0) return GitWorkflowState.AHEAD_OF_REMOTE

        if (status.stagedFiles > 0) return GitWorkflowState.HAS_STAGED

        if (status.modifiedFiles > 0 || status.untrackedFiles > 0) {
            return GitWorkflowState.HAS_CHANGES
        }

        return GitWorkflowState.CLEAN
    }

    /**
     * Get user-friendly explanation of current state
     */
    fun getStateExplanation(status: GitStatus): String {
        val state = determineWorkflowState(status)

        return when (state) {
            GitWorkflowState.NO_REPO ->
                "このフォルダはまだGitで管理されていません。\n「Gitリポジトリを作成」で始めましょう。"

            GitWorkflowState.CLEAN ->
                "すべての変更が保存されています。\n新しい作業を始められます。"

            GitWorkflowState.HAS_CHANGES ->
                "${status.modifiedFiles}個のファイルに変更があります。\n変更をコミットする準備をしましょう。"

            GitWorkflowState.HAS_STAGED ->
                "${status.stagedFiles}個のファイルがコミット待ちです。\nコミットメッセージを書いて保存しましょう。"

            GitWorkflowState.AHEAD_OF_REMOTE ->
                "${status.aheadCommits}個のコミットがまだリモートにありません。\nプッシュして共有しましょう。"

            GitWorkflowState.BEHIND_REMOTE ->
                "リモートに${status.behindCommits}個の新しいコミットがあります。\nPullして最新版を取得しましょう。"

            GitWorkflowState.DIVERGED ->
                "あなたとリモートの両方に変更があります。\n統合が必要です。"

            GitWorkflowState.CONFLICT ->
                "${status.conflictFiles}個のファイルで競合が発生しています。\n手動で解決する必要があります。"
        }
    }
}
