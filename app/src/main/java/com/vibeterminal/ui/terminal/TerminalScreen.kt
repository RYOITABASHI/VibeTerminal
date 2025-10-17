package com.vibeterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeterminal.ui.ime.JapaneseIMEBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel()
) {
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isTranslationVisible by viewModel.isTranslationVisible.collectAsState()
    val currentTranslation by viewModel.currentTranslation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VibeTerminal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // Translation toggle
                    IconButton(onClick = { viewModel.toggleTranslation() }) {
                        Icon(
                            imageVector = if (isTranslationVisible)
                                Icons.Default.Translate
                            else
                                Icons.Default.TranslateOff,
                            contentDescription = "Toggle Translation"
                        )
                    }

                    // Keyboard toggle
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
            // Terminal view
            TerminalView(
                output = terminalOutput,
                onCommand = { command ->
                    viewModel.executeCommand(command)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Translation overlay
            if (isTranslationVisible && currentTranslation != null) {
                TranslationOverlay(
                    translation = currentTranslation!!,
                    onDismiss = { viewModel.dismissTranslation() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TerminalView(
    output: String,
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

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

            // Input field with Japanese IME
            JapaneseIMEBridge(
                value = inputText,
                onValueChange = { inputText = it },
                onCommand = { command ->
                    onCommand(command)
                    inputText = ""
                },
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF252526))
                    .padding(8.dp)
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
    }
}
