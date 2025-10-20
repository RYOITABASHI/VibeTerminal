package com.vibeterminal.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.commonmark.MarkdownParseOptions
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
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

        // Input area
        ChatInputArea(
            viewModel = viewModel,
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "AI アシスタント",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "コードやエラーについて\n何でも聞いてください",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
                    // Render content with Markdown for assistant messages
                    if (!isUser && !message.isError) {
                        MarkdownText(
                            markdown = message.content,
                            color = Color(0xFFA0A0A0)
                        )
                    } else {
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
                    }

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
    when (attachment.type) {
        AttachmentType.IMAGE -> {
            // Show image preview for images
            if (attachment.uri != null) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 180.dp)
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = Uri.parse(attachment.uri),
                        contentDescription = attachment.name,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                // Fallback to badge
                AttachmentBadgeDefault(attachment)
            }
        }
        else -> {
            AttachmentBadgeDefault(attachment)
        }
    }
}

@Composable
private fun AttachmentBadgeDefault(attachment: ChatAttachment) {
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
    viewModel: ChatViewModel,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachTerminalOutput: () -> Unit,
    enabled: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAttachMenu by remember { mutableStateOf(false) }
    var pendingAttachments by remember { mutableStateOf<List<ChatAttachment>>(emptyList()) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val attachment = viewModel.processImageUri(context, uri)
                if (attachment != null) {
                    pendingAttachments = pendingAttachments + attachment
                    Toast.makeText(context, "画像を追加しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Voice input launcher
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()

            if (spokenText != null) {
                onValueChange(value + spokenText)
                Toast.makeText(context, "音声入力完了", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .padding(8.dp)
    ) {
        // Preview attached images
        if (pendingAttachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(pendingAttachments) { attachment ->
                    AttachmentPreview(
                        attachment = attachment,
                        onRemove = {
                            pendingAttachments = pendingAttachments - attachment
                        }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attach menu
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    enabled = enabled
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "ファイルを添付"
                    )
                }

                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("画像を選択", style = TextStyle(fontSize = 10.sp)) },
                        onClick = {
                            showAttachMenu = false
                            imagePickerLauncher.launch("image/*")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ターミナル出力", style = TextStyle(fontSize = 10.sp)) },
                        onClick = {
                            showAttachMenu = false
                            onAttachTerminalOutput()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                        }
                    )
                }
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

            // Voice input button
            IconButton(
                onClick = {
                    try {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(
                                android.speech.RecognizerIntent.EXTRA_LANGUAGE,
                                java.util.Locale.getDefault()
                            )
                            putExtra(
                                android.speech.RecognizerIntent.EXTRA_PROMPT,
                                "話してください..."
                            )
                        }
                        voiceInputLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "音声認識が利用できません", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = enabled
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "音声入力",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Send button
            IconButton(
                onClick = {
                    if (value.isNotBlank() || pendingAttachments.isNotEmpty()) {
                        viewModel.sendMessage(
                            value.ifBlank { if (pendingAttachments.any { it.type == AttachmentType.IMAGE }) "この画像について教えて" else "説明して" },
                            pendingAttachments
                        )
                        onValueChange("")
                        pendingAttachments = emptyList()
                    }
                },
                enabled = enabled && (value.isNotBlank() || pendingAttachments.isNotEmpty())
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "送信"
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    attachment: ChatAttachment,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        when (attachment.type) {
            AttachmentType.IMAGE -> {
                if (attachment.uri != null) {
                    AsyncImage(
                        model = Uri.parse(attachment.uri),
                        contentDescription = attachment.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            else -> {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = attachment.name,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "削除",
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(timestamp))
}

/**
 * Markdown text renderer with syntax highlighting
 */
@Composable
private fun MarkdownText(
    markdown: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val parser = remember { CommonmarkAstNodeParser() }
    val astNode = remember(markdown) { parser.parse(markdown) }

    RichText(
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            color = color
        )
    ) {
        Markdown(
            content = astNode
        )
    }
}

/**
 * Code block with copy button
 */
@Composable
private fun CodeBlock(
    code: String,
    language: String? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        // Header with language and copy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (language != null) {
                Text(
                    language,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color(0xFF808080)
                )
            }
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "コードをコピーしました", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "コピー",
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF808080)
                )
            }
        }

        // Code content
        Text(
            code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = Color(0xFFCCCCCC),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
