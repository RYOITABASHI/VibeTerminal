package com.vibeterminal.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI Chat Side Panel (Copilot-style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(
    viewModel: ChatViewModel,
    onAttachTerminalOutput: () -> String?,
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(currentSession.messages.size) {
        if (currentSession.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(currentSession.messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        ChatHeader(
            onNewChat = { viewModel.newSession() },
            onClearChat = { viewModel.clearSession() }
        )

        Divider()

        // Welcome message or messages list
        if (currentSession.messages.isEmpty()) {
            ChatWelcome(
                onQuickAction = { action ->
                    inputText = action.prompt
                }
            )
        } else {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentSession.messages) { message ->
                    ChatMessageBubble(message)
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "考え中...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Divider()

        // Quick actions
        if (currentSession.messages.isEmpty()) {
            QuickActionsBar(
                onActionClick = { action ->
                    inputText = action.prompt
                }
            )
            Divider()
        }

        // Input area
        ChatInputArea(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            onAttachTerminalOutput = {
                val output = onAttachTerminalOutput()
                if (output != null) {
                    val attachments = listOf(
                        ChatAttachment(
                            type = AttachmentType.TERMINAL_OUTPUT,
                            content = output,
                            name = "terminal_output.txt"
                        )
                    )
                    viewModel.sendMessage(inputText.ifBlank { "このターミナル出力を説明して" }, attachments)
                    inputText = ""
                }
            },
            enabled = !isLoading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHeader(
    onNewChat: () -> Unit,
    onClearChat: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("AI チャット")
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "新しいチャット")
            }
            IconButton(onClick = onClearChat) {
                Icon(Icons.Default.Delete, contentDescription = "クリア")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ChatWelcome(
    onQuickAction: (QuickAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "AI アシスタント",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "コードやエラーについて\n何でも聞いてください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "よく使う機能:",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(12.dp))

        QuickActions.ALL.take(3).forEach { action ->
            QuickActionButton(
                action = action,
                onClick = { onQuickAction(action) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickActionsBar(
    onActionClick: (QuickAction) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(QuickActions.ALL) { action ->
            FilterChip(
                selected = false,
                onClick = { onActionClick(action) },
                label = { Text(action.label) },
                leadingIcon = {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(action.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else if (message.isError)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else if (message.isError)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show attachments
                    message.attachments.forEach { attachment ->
                        Spacer(Modifier.height(8.dp))
                        AttachmentBadge(attachment)
                    }
                }
            }

            Text(
                formatTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AttachmentBadge(attachment: ChatAttachment) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                when (attachment.type) {
                    AttachmentType.FILE -> Icons.Default.InsertDriveFile
                    AttachmentType.IMAGE -> Icons.Default.Image
                    AttachmentType.TERMINAL_OUTPUT -> Icons.Default.Terminal
                    AttachmentType.CODE_SNIPPET -> Icons.Default.Code
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                attachment.name ?: "attachment",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachTerminalOutput: () -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attach button
            IconButton(
                onClick = onAttachTerminalOutput,
                enabled = enabled
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "ターミナル出力を添付"
                )
            }

            // Input field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("メッセージを入力...") },
                maxLines = 4,
                enabled = enabled
            )

            // Send button
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "送信"
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(timestamp))
}
