package com.vibeterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeterminal.ui.keyboard.VirtualKeyboard
import com.vibeterminal.ui.keyboard.SpecialKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel(),
    settingsViewModel: com.vibeterminal.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isKeyboardVisible by viewModel.isKeyboardVisible.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VibeTerminal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // Keyboard toggle (most important)
                    IconButton(onClick = { viewModel.toggleKeyboard() }) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Terminal view - full screen
            TerminalView(
                output = terminalOutput,
                onCommand = { command ->
                    viewModel.executeCommand(command)
                },
                onSendInput = { input ->
                    viewModel.executeCommand(input)
                },
                isKeyboardVisible = isKeyboardVisible,
                onSpecialKey = { key ->
                    handleSpecialKey(key, viewModel)
                },
                onKeyPress = { key ->
                    viewModel.sendSpecialKey(key)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TerminalView(
    output: String,
    onCommand: (String) -> Unit,
    onSendInput: (String) -> Unit = {},
    isKeyboardVisible: Boolean = false,
    onSpecialKey: (SpecialKey) -> Unit = {},
    onKeyPress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Optimize recomposition by tracking output changes
    val outputState by remember(output) {
        mutableStateOf(output)
    }

    // Auto-scroll effect when output updates
    LaunchedEffect(output) {
        // Future: Implement auto-scroll to bottom when output changes
    }

    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        // Terminal output (scrollable)
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = output,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFFD4D4D4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }

        // Input area with Japanese IME support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prompt
            Text(
                text = "$ ",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFF4EC9B0),
                modifier = Modifier.padding(end = 4.dp)
            )

            // Input field
            androidx.compose.foundation.text.BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF252526))
                    .padding(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFFD4D4D4)
                ),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4EC9B0))
            )

            // Send button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onCommand(inputText.trim())
                        inputText = ""
                    }
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Command",
                    tint = Color(0xFF4EC9B0)
                )
            }
        }

        // Virtual Keyboard
        if (isKeyboardVisible) {
            VirtualKeyboard(
                onKeyPress = { key ->
                    inputText += key
                    onKeyPress(key)
                },
                onSpecialKey = onSpecialKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * Handle special key presses from virtual keyboard
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
