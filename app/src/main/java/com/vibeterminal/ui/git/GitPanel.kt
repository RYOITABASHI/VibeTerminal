package com.vibeterminal.ui.git

import androidx.compose.animation.*
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
import androidx.compose.ui.window.Dialog

/**
 * Git Workflow Assistant Panel
 * Helps beginners understand and use Git
 */
@Composable
fun GitPanel(
    viewModel: GitViewModel,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gitStatus by viewModel.gitStatus.collectAsState()
    val suggestedActions by viewModel.suggestedActions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            GitPanelHeader(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onRefresh = { viewModel.refreshStatus() },
                isRefreshing = isRefreshing
            )

            // Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Git Status Summary
                    GitStatusSummary(gitStatus)

                    if (gitStatus.isRepository) {
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))

                        // State Explanation
                        Text(
                            GitActionSuggestions.getStateExplanation(gitStatus),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (suggestedActions.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))

                            Text(
                                "💡 おすすめのアクション",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(Modifier.height(8.dp))

                            // Suggested Actions
                            suggestedActions.forEach { action ->
                                GitActionCard(
                                    action = action,
                                    onExecute = { confirmedAction ->
                                        viewModel.executeAction(confirmedAction) { success, output ->
                                            // Execute command in terminal to show output
                                            onExecuteCommand(confirmedAction.command)
                                        }
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitPanelHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Surface(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Git アシスタント",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "次に何をすべきか教えます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "更新",
                    modifier = Modifier.then(
                        if (isRefreshing) Modifier else Modifier
                    )
                )
            }

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "閉じる" else "開く"
            )
        }
    }
}

@Composable
private fun GitStatusSummary(status: GitStatus) {
    if (!status.isRepository) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Gitリポジトリではありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Branch
        status.currentBranch?.let { branch ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "ブランチ: $branch",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // File changes
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (status.modifiedFiles > 0) {
                StatusBadge(
                    icon = Icons.Default.Edit,
                    label = "変更",
                    count = status.modifiedFiles,
                    color = Color(0xFFFFA726)
                )
            }
            if (status.stagedFiles > 0) {
                StatusBadge(
                    icon = Icons.Default.Check,
                    label = "ステージング",
                    count = status.stagedFiles,
                    color = Color(0xFF4CAF50)
                )
            }
            if (status.untrackedFiles > 0) {
                StatusBadge(
                    icon = Icons.Default.QuestionMark,
                    label = "新規",
                    count = status.untrackedFiles,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Remote status
        if (status.hasRemote) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (status.aheadCommits > 0) {
                    StatusBadge(
                        icon = Icons.Default.CloudUpload,
                        label = "プッシュ待ち",
                        count = status.aheadCommits,
                        color = Color(0xFF2196F3)
                    )
                }
                if (status.behindCommits > 0) {
                    StatusBadge(
                        icon = Icons.Default.CloudDownload,
                        label = "プル必要",
                        count = status.behindCommits,
                        color = Color(0xFFFF5722)
                    )
                }
            }
        }

        // Conflicts
        if (status.conflictFiles > 0) {
            StatusBadge(
                icon = Icons.Default.Warning,
                label = "競合",
                count = status.conflictFiles,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                "$label: $count",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun GitActionCard(
    action: GitAction,
    onExecute: (GitAction) -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }

    Card(
        onClick = {
            if (action.requiresConfirmation) {
                showConfirmation = true
            } else {
                onExecute(action)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (action.isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    action.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }

    // Confirmation dialog
    if (showConfirmation) {
        GitActionConfirmationDialog(
            action = action,
            onConfirm = {
                showConfirmation = false
                onExecute(action)
            },
            onDismiss = { showConfirmation = false }
        )
    }
}

@Composable
private fun GitActionConfirmationDialog(
    action: GitAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (action.isDestructive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        action.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    action.explanation,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        action.command,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("キャンセル")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = if (action.isDestructive)
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        else
                            ButtonDefaults.buttonColors()
                    ) {
                        Text("実行")
                    }
                }
            }
        }
    }
}
