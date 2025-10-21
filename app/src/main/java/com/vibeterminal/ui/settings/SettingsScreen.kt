package com.vibeterminal.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val translationEnabled by viewModel.translationEnabled.collectAsState()
    val llmApiKey by viewModel.llmApiKey.collectAsState()
    val useAiTranslation by viewModel.useAiTranslation.collectAsState()
    val openAiApiKey by viewModel.openAiApiKey.collectAsState()
    val openAiModel by viewModel.openAiModel.collectAsState()

    var showToolsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            SettingsSection(title = "Appearance") {
                SwitchPreference(
                    title = "Dark Theme",
                    subtitle = "Use dark theme for terminal",
                    icon = Icons.Default.DarkMode,
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )

                SliderPreference(
                    title = "Font Size",
                    subtitle = "${fontSize}sp",
                    icon = Icons.Default.TextFields,
                    value = fontSize.toFloat(),
                    onValueChange = { viewModel.setFontSize(it.toInt()) },
                    valueRange = 10f..24f
                )
            }

            // Translation Section
            SettingsSection(title = "Translation") {
                SwitchPreference(
                    title = "Enable Translation",
                    subtitle = "Translate command output to Japanese",
                    icon = Icons.Default.Translate,
                    checked = translationEnabled,
                    onCheckedChange = { viewModel.setTranslationEnabled(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SwitchPreference(
                    title = "Use AI Translation",
                    subtitle = "Use Gemini AI for advanced translation (requires API key)",
                    icon = Icons.Default.AutoAwesome,
                    checked = useAiTranslation,
                    onCheckedChange = { viewModel.setUseAiTranslation(it) }
                )

                TextFieldPreference(
                    title = "Gemini API Key",
                    subtitle = "Google AI API key for Gemini translation",
                    icon = Icons.Default.Key,
                    value = llmApiKey,
                    onValueChange = { viewModel.setLlmApiKey(it) },
                    isPassword = true
                )
            }

            // AI Chat Section
            SettingsSection(title = "AI Chat") {
                TextFieldPreference(
                    title = "OpenAI API Key",
                    subtitle = "API key for ChatGPT integration",
                    icon = Icons.Default.Key,
                    value = openAiApiKey,
                    onValueChange = { viewModel.setOpenAiApiKey(it) },
                    isPassword = true
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                PreferenceItem(
                    title = "Model",
                    subtitle = openAiModel,
                    icon = Icons.Default.SmartToy,
                    onClick = { /* TODO: Model selector dialog */ }
                )
            }

            // Tools Management Section
            SettingsSection(title = "Tools & Shell") {
                PreferenceItem(
                    title = "Manage Tools",
                    subtitle = "Install BusyBox, AI CLI tools, and manage Termux integration",
                    icon = Icons.Default.Build,
                    onClick = { showToolsDialog = true }
                )
            }

            // About Section
            SettingsSection(title = "About") {
                PreferenceItem(
                    title = "Version",
                    subtitle = "0.1.0-alpha",
                    icon = Icons.Default.Info
                )

                PreferenceItem(
                    title = "GitHub",
                    subtitle = "View source code",
                    icon = Icons.Default.Code,
                    onClick = { /* TODO: Open GitHub */ }
                )
            }
        }

        // Tools Management Dialog
        if (showToolsDialog) {
            AlertDialog(
                onDismissRequest = { showToolsDialog = false },
                title = { Text("ツール管理") },
                text = {
                    ToolsManagementScreen()
                },
                confirmButton = {
                    TextButton(onClick = { showToolsDialog = false }) {
                        Text("閉じる")
                    }
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SliderPreference(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldPreference(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            }
        }

        if (expanded) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 8.dp),
                singleLine = true,
                visualTransformation = if (isPassword)
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                else
                    androidx.compose.ui.text.input.VisualTransformation.None
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
