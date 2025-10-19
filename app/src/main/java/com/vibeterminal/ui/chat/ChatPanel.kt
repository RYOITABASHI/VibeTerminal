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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(Color(0xFF000000))
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
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSession.messages) { message ->
                    ChatMessageBubble(message)
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.dp,
                                color = Color(0xFF4EC9B0)
                            )
                            Text(
                                "考え中...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(0xFF808080)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.ChatBubble,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color(0xFF4EC9B0)
            )
            Text(
                "AI",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFFD4D4D4)
            )
        }
        Row {
            IconButton(onClick = onNewChat, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新しいチャット",
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF808080)
                )
            }
            IconButton(onClick = onClearChat, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "クリア",
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF808080)
                )
            }
        }
    }
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
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF4EC9B0)
            )
            Spacer(Modifier.width(3.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 200.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isUser)
                    Color(0xFF1A1A1A)
                else if (message.isError)
                    Color(0xFF2A0A0A)
                else
                    Color(0xFF0A0A0A)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isUser)
                            Color(0xFFD4D4D4)
                        else if (message.isError)
                            Color(0xFFFF6B6B)
                        else
                            Color(0xFFA0A0A0)
                    )

                    // Show attachments
                    message.attachments.forEach { attachment ->
                        Spacer(Modifier.height(4.dp))
                        AttachmentBadge(attachment)
                    }
                }
            }

            Text(
                formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color(0xFF505050),
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )
        }

        if (isUser) {
            Spacer(Modifier.width(3.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF4EC9B0)
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
            .background(Color(0xFF000000))
            .padding(8.dp)
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
                placeholder = { Text("メッセージを入力...", style = TextStyle(fontSize = 10.sp)) },
                maxLines = 3,
                enabled = enabled,
                textStyle = TextStyle(fontSize = 10.sp)
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
