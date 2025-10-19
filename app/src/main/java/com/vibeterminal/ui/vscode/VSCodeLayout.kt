package com.vibeterminal.ui.vscode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeterminal.ui.terminal.TerminalView
import com.vibeterminal.ui.terminal.TerminalViewModel
import com.vibeterminal.ui.chat.ChatPanel
import com.vibeterminal.ui.chat.ChatViewModel
import com.vibeterminal.ui.keyboard.SpecialKey

/**
 * VS Code-style layout
 * Left: Menu bar (10%)
 * Center: Terminal (70%)
 * Right: Chat panel (20%)
 */
@Composable
fun VSCodeLayout(
    terminalViewModel: TerminalViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val terminalOutput by terminalViewModel.terminalOutput.collectAsState()
    val isKeyboardVisible by terminalViewModel.isKeyboardVisible.collectAsState()

    var selectedMenu by remember { mutableStateOf<MenuSection>(MenuSection.TERMINAL) }
    var showPopup by remember { mutableStateOf<MenuSection?>(null) }

    Row(modifier = modifier.fillMaxSize()) {
        // Left sidebar - Menu bar (10%)
        MenuSidebar(
            selectedMenu = selectedMenu,
            onMenuSelected = { menu ->
                if (menu == selectedMenu) {
                    // Toggle popup
                    showPopup = if (showPopup == menu) null else menu
                } else {
                    selectedMenu = menu
                    showPopup = null
                }
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
        )

        // Center - Terminal (70%)
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
        ) {
            TerminalView(
                output = terminalOutput,
                onCommand = { terminalViewModel.executeCommand(it) },
                isKeyboardVisible = isKeyboardVisible,
                onSpecialKey = { key -> handleSpecialKey(key, terminalViewModel) },
                onKeyPress = { terminalViewModel.sendSpecialKey(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Popup panels
            showPopup?.let { popup ->
                PopupPanel(
                    section = popup,
                    onDismiss = { showPopup = null },
                    onExecuteCommand = { terminalViewModel.executeCommand(it) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 60.dp)
                        .width(300.dp)
                        .fillMaxHeight()
                )
            }
        }

        // Right - Chat panel (20%)
        Box(
            modifier = Modifier
                .weight(0.2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ChatPanel(
                viewModel = chatViewModel,
                onAttachTerminalOutput = { terminalOutput.takeLast(500) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Left sidebar menu
 */
@Composable
fun MenuSidebar(
    selectedMenu: MenuSection,
    onMenuSelected: (MenuSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
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
 * Menu button
 */
@Composable
fun MenuButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Popup panel for menu features
 */
@Composable
fun PopupPanel(
    section: MenuSection,
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ターミナル設定", style = MaterialTheme.typography.titleMedium)
        Text("フォントサイズ、カラースキーム、エンコーディングなどの設定")
        // Placeholder for future implementation
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("MCP管理", style = MaterialTheme.typography.titleMedium)
        Text("Model Context Protocol サーバーの管理")
        Button(onClick = { onExecuteCommand("mcp list") }) {
            Text("MCPサーバー一覧")
        }
    }
}

/**
 * Git panel
 */
@Composable
fun GitPanel(onExecuteCommand: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Git", style = MaterialTheme.typography.titleMedium)
        Text("Gitリポジトリの状態と操作")
        Button(onClick = { onExecuteCommand("git status") }) {
            Text("Git Status")
        }
        Button(onClick = { onExecuteCommand("git log --oneline -10") }) {
            Text("最新コミット")
        }
    }
}

/**
 * Keyboard settings panel
 */
@Composable
fun KeyboardSettingsPanel() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("キーボード設定", style = MaterialTheme.typography.titleMedium)
        Text("仮想キーボードのレイアウトとショートカット設定")
    }
}

/**
 * App settings panel
 */
@Composable
fun AppSettingsPanel() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("アプリ設定", style = MaterialTheme.typography.titleMedium)
        Text("全般設定、テーマ、APIキーなどの管理")
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
