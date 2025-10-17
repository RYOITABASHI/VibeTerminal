package com.vibeterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.vibeterminal.ui.aicli.*
import com.vibeterminal.ui.mcp.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel(),
    settingsViewModel: com.vibeterminal.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    mcpViewModel: MCPViewModel = viewModel()
) {
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isTranslationVisible by viewModel.isTranslationVisible.collectAsState()
    val currentTranslation by viewModel.currentTranslation.collectAsState()
    val filePickerTrigger by viewModel.filePickerTrigger.collectAsState()
    val cameraUri by viewModel.cameraUri.collectAsState()

    // Get settings
    val geminiApiKey by settingsViewModel.llmApiKey.collectAsState()
    val useAiTranslation by settingsViewModel.useAiTranslation.collectAsState()

    val context = LocalContext.current

    // Initialize ViewModel with context and settings
    LaunchedEffect(Unit) {
        // Translation patterns are in assets/translations
        val patternsDir = java.io.File(context.applicationInfo.dataDir, "translations")
        viewModel.initialize(patternsDir, context, geminiApiKey, useAiTranslation)
    }

    // Update settings when they change
    LaunchedEffect(geminiApiKey, useAiTranslation) {
        viewModel.updateSettings(geminiApiKey, useAiTranslation)
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onCameraPhotoTaken()
        }
    }

    // Handle file picker triggers
    LaunchedEffect(filePickerTrigger) {
        when (filePickerTrigger) {
            FilePickerType.FILE -> filePickerLauncher.launch("*/*")
            FilePickerType.IMAGE -> imagePickerLauncher.launch("image/*")
            FilePickerType.CAMERA -> {
                // Create URI for camera photo
                cameraUri?.let { uri ->
                    cameraLauncher.launch(uri)
                }
            }
            FilePickerType.NONE -> {}
        }
    }

    // MCP UI state
    var showMCPPanel by remember { mutableStateOf(false) }
    var selectedMCPTool by remember { mutableStateOf<Pair<MCPServer, MCPTool>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VibeTerminal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    // MCP servers toggle
                    IconButton(onClick = { showMCPPanel = true }) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = "MCP Servers"
                        )
                    }

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

                    // File picker menu
                    var showFileMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showFileMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File"
                        )
                    }

                    // File picker dropdown menu
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📷 カメラで撮影") },
                            onClick = {
                                showFileMenu = false
                                viewModel.requestCamera()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🖼️ 画像を選択") },
                            onClick = {
                                showFileMenu = false
                                viewModel.requestImagePicker()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📁 ファイルを選択") },
                            onClick = {
                                showFileMenu = false
                                viewModel.requestFilePicker()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MCP Tool Quick Access
            MCPToolQuickAccess(
                viewModel = mcpViewModel,
                onToolSelected = { server, tool ->
                    selectedMCPTool = server to tool
                },
                modifier = Modifier.fillMaxWidth()
            )

            // AI CLI Summary Panel
            val analyzer = remember { AICLIOutputAnalyzer() }
            val summary = remember(terminalOutput) { analyzer.analyze(terminalOutput) }

            AICLISummaryPanel(
                summary = summary,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Terminal view
                TerminalView(
                    output = terminalOutput,
                    onCommand = { command ->
                        viewModel.executeCommand(command)
                    },
                    onSendInput = { input ->
                        viewModel.executeCommand(input)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Gesture overlay
                GestureOverlay(
                    onGesture = { gesture ->
                        when (gesture) {
                            GestureAction.SWIPE_RIGHT -> viewModel.executeCommand("yes")
                            GestureAction.SWIPE_LEFT -> viewModel.executeCommand("no")
                            GestureAction.SWIPE_UP -> {
                                // Copy last prompt - TODO: implement
                            }
                            GestureAction.LONG_PRESS -> {
                                // Show translation - already handled by overlay
                            }
                            GestureAction.DOUBLE_TAP -> {
                                // Send Ctrl+C - TODO: implement
                            }
                        }
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

    // MCP Panel Dialog (outside Scaffold)
    if (showMCPPanel) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showMCPPanel = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column {
                    TopAppBar(
                        title = { Text("MCPサーバー管理") },
                        navigationIcon = {
                            IconButton(onClick = { showMCPPanel = false }) {
                                Icon(Icons.Default.Close, contentDescription = "閉じる")
                            }
                        }
                    )

                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            MCPServerPanel(
                                viewModel = mcpViewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            MCPLogsViewer(
                                viewModel = mcpViewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // MCP Tool Execution Dialog (outside Scaffold)
    selectedMCPTool?.let { (server, tool) ->
        MCPToolExecutionDialog(
            server = server,
            tool = tool,
            viewModel = mcpViewModel,
            onDismiss = { selectedMCPTool = null },
            onCommandGenerated = { command ->
                viewModel.executeCommand(command)
                selectedMCPTool = null
            }
        )
    }
}

@Composable
fun TerminalView(
    output: String,
    onCommand: (String) -> Unit,
    onSendInput: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        // Prompt Template Bar
        PromptTemplateBar(
            onTemplateSelected = { template ->
                inputText = template
            }
        )

        Spacer(Modifier.height(8.dp))
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
