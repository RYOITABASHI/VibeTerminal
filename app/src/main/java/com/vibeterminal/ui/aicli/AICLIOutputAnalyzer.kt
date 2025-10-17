package com.vibeterminal.ui.aicli

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AI CLI output status
 */
enum class AICLIStatus {
    THINKING,    // AIが考え中
    EXECUTING,   // コマンド実行中
    WAITING,     // ユーザー入力待ち
    ERROR,       // エラー発生
    SUCCESS,     // 成功
    IDLE         // 待機中
}

/**
 * AI CLI output summary
 */
data class AICLIOutputSummary(
    val currentStatus: AICLIStatus,
    val summary: String,
    val nextAction: String?,
    val progress: Float?,
    val warning: String?,
    val currentTasks: List<TaskInfo> = emptyList()
)

data class TaskInfo(
    val name: String,
    val completed: Boolean
)

/**
 * Analyzes AI CLI output and generates user-friendly summary
 */
class AICLIOutputAnalyzer {
    fun analyze(output: String): AICLIOutputSummary {
        val lowerOutput = output.lowercase()

        // ステータス判定
        val status = when {
            lowerOutput.contains("error") || lowerOutput.contains("failed") -> AICLIStatus.ERROR
            lowerOutput.contains("success") || lowerOutput.contains("✓") || lowerOutput.contains("done") -> AICLIStatus.SUCCESS
            lowerOutput.contains("analyzing") || lowerOutput.contains("thinking") -> AICLIStatus.THINKING
            lowerOutput.contains("installing") || lowerOutput.contains("creating") || lowerOutput.contains("building") -> AICLIStatus.EXECUTING
            lowerOutput.contains("?") || lowerOutput.contains("(y/n)") -> AICLIStatus.WAITING
            else -> AICLIStatus.IDLE
        }

        // サマリー生成
        val summary = generateSummary(output, status)

        // 次のアクション
        val nextAction = generateNextAction(output, status)

        // 進捗
        val progress = extractProgress(output)

        // 警告
        val warning = extractWarning(output)

        // タスク一覧
        val tasks = extractTasks(output)

        return AICLIOutputSummary(
            currentStatus = status,
            summary = summary,
            nextAction = nextAction,
            progress = progress,
            warning = warning,
            currentTasks = tasks
        )
    }

    private fun generateSummary(output: String, status: AICLIStatus): String {
        val lines = output.lines().takeLast(10)

        return when (status) {
            AICLIStatus.THINKING -> {
                "AIがプロジェクトを分析しています..."
            }
            AICLIStatus.EXECUTING -> {
                when {
                    output.contains("Creating files") || output.contains("creating") -> "ファイルを作成しています"
                    output.contains("Installing") || output.contains("installing") -> "パッケージをインストールしています"
                    output.contains("Building") || output.contains("building") -> "ビルド中です"
                    output.contains("Running tests") -> "テストを実行しています"
                    else -> "コマンドを実行しています"
                }
            }
            AICLIStatus.WAITING -> {
                "確認が必要です。続行しますか？"
            }
            AICLIStatus.ERROR -> {
                "エラーが発生しました"
            }
            AICLIStatus.SUCCESS -> {
                "完了しました！"
            }
            AICLIStatus.IDLE -> {
                "待機中"
            }
        }
    }

    private fun generateNextAction(output: String, status: AICLIStatus): String? {
        return when (status) {
            AICLIStatus.THINKING -> "分析が完了したらファイルを作成します"
            AICLIStatus.EXECUTING -> "完了までお待ちください"
            AICLIStatus.WAITING -> "右スワイプでYes、左スワイプでNoを送信できます"
            AICLIStatus.ERROR -> "エラー内容を確認して修正します"
            AICLIStatus.SUCCESS -> "プロジェクトの準備ができました"
            AICLIStatus.IDLE -> null
        }
    }

    private fun extractProgress(output: String): Float? {
        // パーセンテージを探す
        val percentRegex = Regex("""(\d+)%""")
        val match = percentRegex.findAll(output).lastOrNull()
        return match?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
    }

    private fun extractWarning(output: String): String? {
        return when {
            output.contains("This may take a while", ignoreCase = true) -> "この操作は時間がかかります"
            output.contains("large file", ignoreCase = true) -> "大きなファイルをダウンロードしています"
            else -> null
        }
    }

    private fun extractTasks(output: String): List<TaskInfo> {
        val tasks = mutableListOf<TaskInfo>()

        // "✓ " または "- " で始まる行を探す
        output.lines().forEach { line ->
            when {
                line.contains("✓") -> {
                    val taskName = line.substringAfter("✓").trim()
                    if (taskName.isNotEmpty()) {
                        tasks.add(TaskInfo(taskName, true))
                    }
                }
                line.trim().startsWith("- ") -> {
                    val taskName = line.substringAfter("- ").trim()
                    if (taskName.isNotEmpty()) {
                        tasks.add(TaskInfo(taskName, false))
                    }
                }
            }
        }

        return tasks
    }
}

@Composable
fun AICLISummaryPanel(
    summary: AICLIOutputSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (summary.currentStatus) {
                AICLIStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                AICLIStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                AICLIStatus.WAITING -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ステータスヘッダー
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusIcon(summary.currentStatus)
                Text(
                    "いま何をしている？",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            // サマリー
            Text(
                summary.summary,
                style = MaterialTheme.typography.titleMedium
            )

            // 警告
            summary.warning?.let { warning ->
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 進捗バー
            summary.progress?.let { progress ->
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${(progress * 100).toInt()}% 完了",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // タスクリスト
            if (summary.currentTasks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.currentTasks.forEach { task ->
                        TaskItem(task)
                    }
                }
            }

            // 次のアクション
            summary.nextAction?.let { nextAction ->
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "次に何が起きる？",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    nextAction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun StatusIcon(status: AICLIStatus) {
    when (status) {
        AICLIStatus.THINKING -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        AICLIStatus.EXECUTING -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        AICLIStatus.WAITING -> Icon(
            Icons.Default.QuestionMark,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        AICLIStatus.ERROR -> Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error
        )
        AICLIStatus.SUCCESS -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF4CAF50)
        )
        AICLIStatus.IDLE -> Icon(
            Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskItem(task: TaskInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (task.completed) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (task.completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            task.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
