package com.vibeterminal.ui.terminal

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel(),
    settingsViewModel: com.vibeterminal.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isKeyboardVisible by viewModel.isKeyboardVisible.collectAsState()

    val llmApiKey by settingsViewModel.llmApiKey.collectAsState()
    val useAiTranslation by settingsViewModel.useAiTranslation.collectAsState()

    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        val patternsDir = java.io.File(context.filesDir, "translations")
        viewModel.initialize(
            patternsDir = patternsDir,
            context = context,
            apiKey = llmApiKey,
            useAi = useAiTranslation
        )
    }

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
                viewModel = viewModel,
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
    viewModel: TerminalViewModel,
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
    val scope = rememberCoroutineScope()
    var showAttachMenu by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val path = getFilePathFromUri(context, uri)
                    // Insert file path into input text
                    inputText = if (inputText.isNotEmpty()) {
                        "$inputText \"$path\""
                    } else {
                        "\"$path\""
                    }
                    Toast.makeText(context, "ファイルパスを追加しました", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "ファイル取得に失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val path = getFilePathFromUri(context, uri)
                    // Insert image path into input text
                    inputText = if (inputText.isNotEmpty()) {
                        "$inputText \"$path\""
                    } else {
                        "\"$path\""
                    }
                    Toast.makeText(context, "画像パスを追加しました", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "画像取得に失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera launcher
    val cameraUri by viewModel.cameraUri.collectAsState()
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraUri?.let { uri ->
                scope.launch {
                    try {
                        val path = getFilePathFromUri(context, uri)
                        inputText = if (inputText.isNotEmpty()) {
                            "$inputText \"$path\""
                        } else {
                            "\"$path\""
                        }
                        Toast.makeText(context, "写真パスを追加しました", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "写真取得に失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Observe file picker trigger
    val filePickerTrigger by viewModel.filePickerTrigger.collectAsState()
    LaunchedEffect(filePickerTrigger) {
        when (filePickerTrigger) {
            FilePickerType.FILE -> filePickerLauncher.launch("*/*")
            FilePickerType.IMAGE -> imagePickerLauncher.launch("image/*")
            FilePickerType.CAMERA -> cameraUri?.let { cameraLauncher.launch(it) }
            FilePickerType.NONE -> { /* Do nothing */ }
        }
    }

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
            .background(Color(0xFF000000))
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

            // Attach menu button
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "ファイル添付",
                        tint = Color(0xFF4EC9B0),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("ファイル", fontSize = 12.sp) },
                        onClick = {
                            showAttachMenu = false
                            viewModel.requestFilePicker()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("画像", fontSize = 12.sp) },
                        onClick = {
                            showAttachMenu = false
                            viewModel.requestImagePicker()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("カメラ", fontSize = 12.sp) },
                        onClick = {
                            showAttachMenu = false
                            viewModel.requestCamera()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Input field
            androidx.compose.foundation.text.BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF000000))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4EC9B0),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
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
 * Get file path from URI
 */
private fun getFilePathFromUri(context: android.content.Context, uri: Uri): String {
    return when (uri.scheme) {
        "file" -> uri.path ?: uri.toString()
        "content" -> {
            // For content URIs, try to copy to cache and return path
            try {
                val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
                val cacheFile = java.io.File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                cacheFile.absolutePath
            } catch (e: Exception) {
                uri.toString()
            }
        }
        else -> uri.toString()
    }
}

/**
 * Get file name from URI
 */
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var fileName: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
    }
    if (fileName == null) {
        fileName = uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) path.substring(cut + 1) else path
        }
    }
    return fileName
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
