package com.vibeterminal.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeterminal.ui.chat.ChatPanel
import com.vibeterminal.ui.chat.ChatViewModel
import com.vibeterminal.ui.git.GitPanel
import com.vibeterminal.ui.git.GitViewModel
import com.vibeterminal.ui.keyboard.SpecialKey
import java.io.File

/**
 * Adaptive layout for Z Fold6
 * Expanded: Side-by-side panels (Terminal | Chat + Git)
 * Compact: Tabbed layout
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTerminalLayout(
    terminalViewModel: TerminalViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    gitViewModel: GitViewModel = viewModel(),
    settingsViewModel: com.vibeterminal.ui.settings.SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }

    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    // Initialize Git ViewModel
    LaunchedEffect(Unit) {
        val workingDir = File(System.getProperty("user.dir") ?: "/")
        gitViewModel.initialize(workingDir)
    }

    // Sync OpenAI API key to ChatViewModel
    val openAiApiKey by settingsViewModel.openAiApiKey.collectAsState()
    val openAiModel by settingsViewModel.openAiModel.collectAsState()
    LaunchedEffect(openAiApiKey, openAiModel) {
        chatViewModel.setOpenAiApiKey(openAiApiKey)
        chatViewModel.setModel(openAiModel)
    }

    if (isExpanded) {
        ExpandedLayout(
            terminalViewModel = terminalViewModel,
            chatViewModel = chatViewModel,
            gitViewModel = gitViewModel
        )
    } else {
        CompactLayout(
            terminalViewModel = terminalViewModel,
            chatViewModel = chatViewModel,
            gitViewModel = gitViewModel
        )
    }
}

/**
 * Expanded layout (Z Fold6 unfolded)
 * Terminal on left, Chat + Git on right
 */
@Composable
private fun ExpandedLayout(
    terminalViewModel: TerminalViewModel,
    chatViewModel: ChatViewModel,
    gitViewModel: GitViewModel
) {
    val terminalOutput by terminalViewModel.terminalOutput.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Terminal (60% width)
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            TerminalView(
                viewModel = terminalViewModel,
                output = terminalOutput,
                onCommand = { terminalViewModel.executeCommand(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Right: Side panels (40% width)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            // Top: Git Panel (40% height)
            Box(modifier = Modifier.weight(0.4f)) {
                GitPanel(
                    viewModel = gitViewModel,
                    onExecuteCommand = { terminalViewModel.executeCommand(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Divider()

            // Bottom: Chat Panel (60% height)
            Box(modifier = Modifier.weight(0.6f)) {
                ChatPanel(
                    viewModel = chatViewModel,
                    onAttachTerminalOutput = { terminalOutput.takeLast(500) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Compact layout (Z Fold6 folded or regular phone)
 * Tabbed interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    terminalViewModel: TerminalViewModel,
    chatViewModel: ChatViewModel,
    gitViewModel: GitViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val terminalOutput by terminalViewModel.terminalOutput.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("ターミナル") },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("AI チャット") },
                icon = { Icon(Icons.Default.ChatBubble, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Git") },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null) }
            )
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> TerminalView(
                    viewModel = terminalViewModel,
                    output = terminalOutput,
                    onCommand = { terminalViewModel.executeCommand(it) },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ChatPanel(
                    viewModel = chatViewModel,
                    onAttachTerminalOutput = { terminalOutput.takeLast(500) },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> GitPanel(
                    viewModel = gitViewModel,
                    onExecuteCommand = { terminalViewModel.executeCommand(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
