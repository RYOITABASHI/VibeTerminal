package com.vibeterminal.ui.vscode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeterminal.ui.terminal.TerminalView
import com.vibeterminal.ui.terminal.TerminalViewModel
import com.vibeterminal.ui.chat.ChatPanel
import com.vibeterminal.ui.chat.ChatViewModel
import com.vibeterminal.ui.keyboard.SpecialKey

/**
 * VS Code-style layout
 * Responsive layout:
 * - Wide screen (>600dp): Left sidebar + Terminal + Chat (horizontal)
 * - Narrow screen (≤600dp): Left sidebar + Terminal/Chat (vertical split)
 */
@Composable
fun VSCodeLayout(
    terminalViewModel: TerminalViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val terminalOutput by terminalViewModel.terminalOutput.collectAsState()
    val isKeyboardVisible by terminalViewModel.isKeyboardVisible.collectAsState()

    var selectedMenu by remember { mutableStateOf<MenuSection?>(null) }
    var showPopup by remember { mutableStateOf<MenuSection?>(null) }

    // リサイズ可能な境界線用のstate
    var terminalWeight by remember { mutableStateOf(0.7f) }
    var verticalWeight by remember { mutableStateOf(0.6f) }

    // 画面幅を取得（Zfoldカバー画面対応）
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isNarrowScreen = screenWidthDp <= 600.dp

    Row(modifier = modifier.fillMaxSize()) {
        // Left sidebar - Menu bar (compact)
        MenuSidebar(
            selectedMenu = selectedMenu,
            onMenuSelected = { menu ->
                // ワンタップで設定画面を開く
                selectedMenu = menu
                showPopup = menu
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFF1A1A1A)
                )
        )

        // Main content area - レイアウトを画面幅に応じて切り替え
        if (isNarrowScreen) {
            // 狭い画面（カバー画面）: 上下分割レイアウト
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                // Terminal (上部)
                Box(
                    modifier = Modifier
                        .weight(verticalWeight)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFF1A1A1A)
                        )
                ) {
                    TerminalView(
                        output = terminalOutput,
                        onCommand = { terminalViewModel.executeCommand(it) },
                        isKeyboardVisible = isKeyboardVisible,
                        onSpecialKey = { key -> handleSpecialKey(key, terminalViewModel) },
                        onKeyPress = { terminalViewModel.sendSpecialKey(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // リサイズ可能な境界線（垂直方向）
                VerticalResizableDivider(
                    onDrag = { delta ->
                        val newWeight = (verticalWeight + delta * 0.001f).coerceIn(0.3f, 0.8f)
                        verticalWeight = newWeight
                    }
                )

                // Chat panel (下部)
                Box(
                    modifier = Modifier
                        .weight(1f - verticalWeight)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF1A1A1A)
                        )
                ) {
                    ChatPanel(
                        viewModel = chatViewModel,
                        onAttachTerminalOutput = { terminalOutput.takeLast(500) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // 広い画面（メイン画面）: 左右分割レイアウト
            // Center - Terminal
            Box(
                modifier = Modifier
                    .weight(terminalWeight)
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1A1A1A)
                    )
            ) {
                TerminalView(
                    output = terminalOutput,
                    onCommand = { terminalViewModel.executeCommand(it) },
                    isKeyboardVisible = isKeyboardVisible,
                    onSpecialKey = { key -> handleSpecialKey(key, terminalViewModel) },
                    onKeyPress = { terminalViewModel.sendSpecialKey(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // リサイズ可能な境界線（水平方向）
            HorizontalResizableDivider(
                onDrag = { delta ->
                    val newWeight = (terminalWeight + delta * 0.001f).coerceIn(0.3f, 0.8f)
                    terminalWeight = newWeight
                }
            )

            // Right - Chat panel
            Box(
                modifier = Modifier
                    .weight(1f - terminalWeight)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1A1A1A)
                    )
            ) {
                ChatPanel(
                    viewModel = chatViewModel,
                    onAttachTerminalOutput = { terminalOutput.takeLast(500) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // 設定パネルをダイアログで表示（全画面対応）
    showPopup?.let { popup ->
        SettingsDialog(
            section = popup,
            onDismiss = { showPopup = null },
            onExecuteCommand = { terminalViewModel.executeCommand(it) },
            isNarrowScreen = isNarrowScreen
        )
    }
}

/**
 * Left sidebar menu
 */
@Composable
fun MenuSidebar(
    selectedMenu: MenuSection?,
    onMenuSelected: (MenuSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF000000))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MenuSection.values().forEach { section ->
            MenuButton(
                icon = section.icon,
                label = section.label,
                isSelected = selectedMenu == section,
                onClick = { onMenuSelected(section) }
            )
        }

        Spacer(Modifier.weight(1f))

        // Settings at bottom
        MenuButton(
            icon = Icons.Default.Settings,
            label = "設定",
            isSelected = selectedMenu == MenuSection.SETTINGS,
            onClick = { onMenuSelected(MenuSection.SETTINGS) }
        )
    }
}

/**
 * Menu button (icon only, compact)
 */
@Composable
fun MenuButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFF1A1A1A)
                else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (isSelected) Color(0xFF4EC9B0) else Color(0xFF808080)
        )
    }
}

/**
 * Settings dialog for menu features (全画面対応)
 */
@Composable
fun SettingsDialog(
    section: MenuSection,
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    isNarrowScreen: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // フルスクリーン対応
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isNarrowScreen) 8.dp else 40.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }

                Divider()

                // Content based on section
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (section) {
                        MenuSection.TERMINAL -> {
                            TerminalSettingsPanel()
                        }
                        MenuSection.AI_ASSIST -> {
                            AIAssistPanel(onExecuteCommand = onExecuteCommand)
                        }
                        MenuSection.MCP -> {
                            MCPPanel(onExecuteCommand = onExecuteCommand)
                        }
                        MenuSection.GIT -> {
                            GitPanel(onExecuteCommand = onExecuteCommand)
                        }
                        MenuSection.KEYBOARD -> {
                            KeyboardSettingsPanel()
                        }
                        MenuSection.SETTINGS -> {
                            AppSettingsPanel()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menu sections
 */
enum class MenuSection(val icon: ImageVector, val label: String) {
    TERMINAL(Icons.Default.Terminal, "ターミナル"),
    AI_ASSIST(Icons.Default.AutoAwesome, "AI補完"),
    MCP(Icons.Default.Cable, "MCP"),
    GIT(Icons.Default.AccountTree, "Git"),
    KEYBOARD(Icons.Default.Keyboard, "キーボード"),
    SETTINGS(Icons.Default.Settings, "設定")
}

/**
 * Terminal settings panel
 */
@Composable
fun TerminalSettingsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsViewModel = remember {
        com.vibeterminal.ui.settings.SettingsViewModel(context)
    }
    val terminalFontSize by settingsViewModel.terminalFontSize.collectAsState()
    val colorScheme by settingsViewModel.terminalColorScheme.collectAsState()
    val showLineNumbers by settingsViewModel.showLineNumbers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ターミナル設定", style = MaterialTheme.typography.titleMedium)

        // Font size
        Text("フォントサイズ: ${terminalFontSize}sp", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = terminalFontSize.toFloat(),
            onValueChange = { settingsViewModel.setTerminalFontSize(it.toInt()) },
            valueRange = 10f..24f,
            steps = 13
        )

        Divider()

        // Color scheme
        Text("カラースキーム", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = colorScheme == "dark",
                onClick = { settingsViewModel.setTerminalColorScheme("dark") },
                label = { Text("Dark") }
            )
            FilterChip(
                selected = colorScheme == "light",
                onClick = { settingsViewModel.setTerminalColorScheme("light") },
                label = { Text("Light") }
            )
            FilterChip(
                selected = colorScheme == "monokai",
                onClick = { settingsViewModel.setTerminalColorScheme("monokai") },
                label = { Text("Monokai") }
            )
        }

        Divider()

        // Show line numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("行番号を表示", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = showLineNumbers,
                onCheckedChange = { settingsViewModel.setShowLineNumbers(it) }
            )
        }
    }
}

/**
 * AI Assist panel
 */
@Composable
fun AIAssistPanel(onExecuteCommand: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("AI補完", style = MaterialTheme.typography.titleMedium)
        Text("プロンプトテンプレートとAI支援機能")
        Button(onClick = { onExecuteCommand("ai-suggest") }) {
            Text("AI提案を取得")
        }
    }
}

/**
 * MCP panel
 */
@Composable
fun MCPPanel(onExecuteCommand: (String) -> Unit) {
    var servers by remember { mutableStateOf(listOf("filesystem", "git", "sqlite")) }
    var selectedServer by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MCP管理", style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "追加",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            "Model Context Protocol サーバー",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // Servers list
        Text("サーバー一覧", style = MaterialTheme.typography.bodyMedium)

        servers.forEach { server ->
            MCPServerCard(
                serverName = server,
                isRunning = false,
                onClick = { selectedServer = server },
                onStart = { onExecuteCommand("mcp start $server") },
                onStop = { onExecuteCommand("mcp stop $server") }
            )
        }

        if (servers.isEmpty()) {
            Text(
                "MCPサーバーがありません",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Divider()

        // Quick actions
        Text("クイックアクション", style = MaterialTheme.typography.bodyMedium)

        OutlinedButton(
            onClick = { onExecuteCommand("mcp list") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("サーバー一覧")
        }

        OutlinedButton(
            onClick = { onExecuteCommand("mcp status") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("ステータス確認")
        }

        // Selected server details
        selectedServer?.let { server ->
            Divider()
            Text("サーバー詳細: $server", style = MaterialTheme.typography.bodyMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "利用可能なツール:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    when (server) {
                        "filesystem" -> {
                            Text("• ファイル読み書き", style = MaterialTheme.typography.bodySmall)
                            Text("• ディレクトリ操作", style = MaterialTheme.typography.bodySmall)
                        }
                        "git" -> {
                            Text("• Git操作", style = MaterialTheme.typography.bodySmall)
                            Text("• ブランチ管理", style = MaterialTheme.typography.bodySmall)
                        }
                        "sqlite" -> {
                            Text("• SQLite操作", style = MaterialTheme.typography.bodySmall)
                            Text("• クエリ実行", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // Add server dialog
    if (showAddDialog) {
        var newServerName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("MCPサーバーを追加") },
            text = {
                OutlinedTextField(
                    value = newServerName,
                    onValueChange = { newServerName = it },
                    label = { Text("サーバー名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newServerName.isNotBlank()) {
                        servers = servers + newServerName
                        showAddDialog = false
                        newServerName = ""
                    }
                }) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun MCPServerCard(
    serverName: String,
    isRunning: Boolean,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Cable,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isRunning) Color(0xFF4EC9B0) else Color(0xFF808080)
                )
                Text(
                    serverName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = if (isRunning) onStop else onStart,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "停止" else "開始",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Git panel
 */
@Composable
fun GitPanel(onExecuteCommand: (String) -> Unit) {
    var currentBranch by remember { mutableStateOf("main") }
    var isRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Git", style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = {
                    isRefreshing = true
                    onExecuteCommand("git status")
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "更新",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Branch info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4EC9B0)
                    )
                    Text(
                        currentBranch,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        Divider()

        // Quick actions
        Text("クイックアクション", style = MaterialTheme.typography.bodyMedium)

        GitQuickAction(
            icon = Icons.Default.Info,
            label = "Status",
            onClick = { onExecuteCommand("git status") }
        )

        GitQuickAction(
            icon = Icons.Default.History,
            label = "Log",
            onClick = { onExecuteCommand("git log --oneline -10") }
        )

        GitQuickAction(
            icon = Icons.Default.Difference,
            label = "Diff",
            onClick = { onExecuteCommand("git diff") }
        )

        Divider()

        // Branching
        Text("ブランチ操作", style = MaterialTheme.typography.bodyMedium)

        GitQuickAction(
            icon = Icons.Default.List,
            label = "ブランチ一覧",
            onClick = { onExecuteCommand("git branch -a") }
        )

        var newBranchName by remember { mutableStateOf("") }
        var showBranchDialog by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showBranchDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("新規ブランチ")
        }

        // Branch dialog
        if (showBranchDialog) {
            AlertDialog(
                onDismissRequest = { showBranchDialog = false },
                title = { Text("新規ブランチ") },
                text = {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("ブランチ名") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBranchName.isNotBlank()) {
                            onExecuteCommand("git checkout -b $newBranchName")
                            showBranchDialog = false
                            newBranchName = ""
                        }
                    }) {
                        Text("作成")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBranchDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }

        Divider()

        // Staging
        Text("ステージング", style = MaterialTheme.typography.bodyMedium)

        GitQuickAction(
            icon = Icons.Default.Add,
            label = "全ての変更をステージ",
            onClick = { onExecuteCommand("git add .") }
        )

        GitQuickAction(
            icon = Icons.Default.Check,
            label = "Commit",
            onClick = { onExecuteCommand("git commit -m \"Update\"") }
        )

        GitQuickAction(
            icon = Icons.Default.Upload,
            label = "Push",
            onClick = { onExecuteCommand("git push") }
        )

        GitQuickAction(
            icon = Icons.Default.Download,
            label = "Pull",
            onClick = { onExecuteCommand("git pull") }
        )
    }
}

@Composable
private fun GitQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Keyboard settings panel
 */
@Composable
fun KeyboardSettingsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsViewModel = remember {
        com.vibeterminal.ui.settings.SettingsViewModel(context)
    }
    val keyboardHeight by settingsViewModel.keyboardHeight.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("キーボード設定", style = MaterialTheme.typography.titleMedium)

        // Keyboard height
        Text("キーボード高さ: ${keyboardHeight}dp", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = keyboardHeight.toFloat(),
            onValueChange = { settingsViewModel.setKeyboardHeight(it.toInt()) },
            valueRange = 60f..180f,
            steps = 11
        )

        Divider()

        // Shortcuts info
        Text("ショートカットキー", style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShortcutInfo("ESC", "エスケープキー")
            ShortcutInfo("Ctrl+C", "プロセス中断")
            ShortcutInfo("Ctrl+D", "入力終了/ログアウト")
            ShortcutInfo("Tab", "オートコンプリート")
            ShortcutInfo("↑↓", "コマンド履歴")
        }
    }
}

@Composable
private fun ShortcutInfo(key: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = Color(0xFF4EC9B0)
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * App settings panel
 */
@Composable
fun AppSettingsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsViewModel = remember {
        com.vibeterminal.ui.settings.SettingsViewModel(context)
    }
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val translationEnabled by settingsViewModel.translationEnabled.collectAsState()
    val useAiTranslation by settingsViewModel.useAiTranslation.collectAsState()
    val llmApiKey by settingsViewModel.llmApiKey.collectAsState()
    val openAiApiKey by settingsViewModel.openAiApiKey.collectAsState()
    val openAiModel by settingsViewModel.openAiModel.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showOpenAiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("アプリ設定", style = MaterialTheme.typography.titleMedium)

        // Dark theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ダークテーマ", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { settingsViewModel.setDarkTheme(it) }
            )
        }

        Divider()

        // Translation
        Text("翻訳設定", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("翻訳を有効化", style = MaterialTheme.typography.bodySmall)
            Switch(
                checked = translationEnabled,
                onCheckedChange = { settingsViewModel.setTranslationEnabled(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI翻訳を使用", style = MaterialTheme.typography.bodySmall)
            Switch(
                checked = useAiTranslation,
                onCheckedChange = { settingsViewModel.setUseAiTranslation(it) }
            )
        }

        if (useAiTranslation) {
            OutlinedButton(
                onClick = { showApiKeyDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (llmApiKey.isEmpty()) "Gemini APIキーを設定" else "APIキー設定済み")
            }
        }

        Divider()

        // OpenAI Settings
        Text("OpenAI設定", style = MaterialTheme.typography.bodyMedium)

        // API認証に関する説明カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4EC9B0)
                    )
                    Text(
                        "API認証について",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4EC9B0)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "OpenAI APIは APIキー認証が必須です。ChatGPTのサブスクアカウントとは別の課金体系で、従量課金の契約が必要です。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showOpenAiKeyDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (openAiApiKey.isEmpty()) "OpenAI APIキーを設定" else "APIキー設定済み")
        }

        Text("モデル", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = openAiModel == "gpt-4o-mini",
                onClick = { settingsViewModel.setOpenAiModel("gpt-4o-mini") },
                label = { Text("GPT-4o mini") }
            )
            FilterChip(
                selected = openAiModel == "gpt-4o",
                onClick = { settingsViewModel.setOpenAiModel("gpt-4o") },
                label = { Text("GPT-4o") }
            )
        }

        Divider()

        // App info
        Text("アプリ情報", style = MaterialTheme.typography.bodyMedium)
        Text("Version: 0.1.0-alpha", style = MaterialTheme.typography.bodySmall)
        Text("VibeTerminal", style = MaterialTheme.typography.bodySmall)
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(llmApiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini APIキー") },
            text = {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("APIキー") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.setLlmApiKey(keyInput)
                    showApiKeyDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // OpenAI Key Dialog
    if (showOpenAiKeyDialog) {
        var keyInput by remember { mutableStateOf(openAiApiKey) }
        AlertDialog(
            onDismissRequest = { showOpenAiKeyDialog = false },
            title = { Text("OpenAI APIキー") },
            text = {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("APIキー") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.setOpenAiApiKey(keyInput)
                    showOpenAiKeyDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenAiKeyDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

/**
 * Handle special key presses
 */
private fun handleSpecialKey(key: SpecialKey, viewModel: TerminalViewModel) {
    when (key) {
        SpecialKey.ESC -> viewModel.sendSpecialKey("\u001B")
        SpecialKey.TAB -> viewModel.sendSpecialKey("\t")
        SpecialKey.ENTER -> viewModel.sendSpecialKey("\n")
        SpecialKey.ARROW_UP -> viewModel.sendSpecialKey("\u001B[A")
        SpecialKey.ARROW_DOWN -> viewModel.sendSpecialKey("\u001B[B")
        SpecialKey.ARROW_RIGHT -> viewModel.sendSpecialKey("\u001B[C")
        SpecialKey.ARROW_LEFT -> viewModel.sendSpecialKey("\u001B[D")
        SpecialKey.CTRL_C -> viewModel.sendSpecialKey("\u0003")
        SpecialKey.CTRL_D -> viewModel.sendSpecialKey("\u0004")
        SpecialKey.CTRL_Z -> viewModel.sendSpecialKey("\u001A")
        SpecialKey.PAGE_UP -> viewModel.sendSpecialKey("\u001B[5~")
        SpecialKey.PAGE_DOWN -> viewModel.sendSpecialKey("\u001B[6~")
        SpecialKey.HOME -> viewModel.sendSpecialKey("\u001B[H")
        SpecialKey.END -> viewModel.sendSpecialKey("\u001B[F")
    }
}

/**
 * リサイズ可能な境界線（水平方向）
 */
@Composable
private fun HorizontalResizableDivider(
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    ) {
        // 中央にドラッグハンドルを表示
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color(0xFF4EC9B0))
                .align(Alignment.Center)
        )
    }
}

/**
 * リサイズ可能な境界線（垂直方向）
 */
@Composable
private fun VerticalResizableDivider(
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .height(8.dp)
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    ) {
        // 中央にドラッグハンドルを表示
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(Color(0xFF4EC9B0))
                .align(Alignment.Center)
        )
    }
}
