package com.vibeterminal.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibeterminal.core.shell.ShellType
import com.vibeterminal.core.termux.TermuxIntegration
import com.vibeterminal.core.termux.TermuxAccessibilityStatus
import com.vibeterminal.core.tools.BusyBoxManager
import com.vibeterminal.core.tools.CLIToolsManager
import com.vibeterminal.core.tools.CLITool
import kotlinx.coroutines.launch

@Composable
fun ToolsManagementScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val busyBoxManager = remember { BusyBoxManager(context) }
    val cliToolsManager = remember { CLIToolsManager(context) }
    val termuxIntegration = remember { TermuxIntegration(context) }

    var busyBoxInstalled by remember { mutableStateOf(busyBoxManager.isInstalled()) }
    var busyBoxVersion by remember { mutableStateOf<String?>(null) }
    var installProgress by remember { mutableStateOf(0) }
    var installMessage by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }

    var termuxStatus by remember { mutableStateOf(termuxIntegration.getStatus()) }
    var installedTools by remember { mutableStateOf(cliToolsManager.getInstalledTools()) }
    var availableTools by remember { mutableStateOf(cliToolsManager.getAvailableTools()) }

    LaunchedEffect(Unit) {
        busyBoxVersion = busyBoxManager.getVersion()
        termuxStatus = termuxIntegration.getStatus()
        installedTools = cliToolsManager.getInstalledTools()
        availableTools = cliToolsManager.getAvailableTools()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Termux Status Section
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, "Termux")
                        Text("Termux ステータス", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("インストール済", if (termuxStatus.installed) "はい" else "いいえ")
                    if (termuxStatus.installed) {
                        InfoRow("バージョン", termuxStatus.version ?: "不明")
                        InfoRow("アクセス", termuxStatus.accessibilityStatus.getDescription())
                        if (termuxStatus.installedPackages > 0) {
                            InfoRow("パッケージ", "${termuxStatus.installedPackages}個")
                        }
                    }

                    if (!termuxStatus.installed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { termuxIntegration.openTermuxPlayStore() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Termuxをインストール")
                        }
                    }
                }
            }
        }

        // BusyBox Section
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Build, "BusyBox")
                        Text("BusyBox", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (busyBoxInstalled) {
                        InfoRow("ステータス", "インストール済")
                        busyBoxVersion?.let {
                            InfoRow("バージョン", it)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    busyBoxManager.uninstall()
                                    busyBoxInstalled = false
                                    busyBoxVersion = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("アンインストール")
                        }
                    } else {
                        Text(
                            "BusyBoxは350以上のUnixコマンドを提供します",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isInstalling) {
                            LinearProgressIndicator(
                                progress = installProgress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                installMessage,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    isInstalling = true
                                    scope.launch {
                                        busyBoxManager.install { progress, message ->
                                            installProgress = progress
                                            installMessage = message
                                        }.onSuccess {
                                            busyBoxInstalled = true
                                            busyBoxVersion = busyBoxManager.getVersion()
                                            isInstalling = false
                                        }.onFailure {
                                            installMessage = "エラー: ${it.message}"
                                            isInstalling = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("BusyBoxをインストール (~2.5MB)")
                            }
                        }
                    }
                }
            }
        }

        // AI CLI Tools Section
        item {
            Text("AI CLIツール", style = MaterialTheme.typography.titleMedium)
        }

        // Installed Tools
        items(installedTools) { tool ->
            CLIToolCard(
                tool = tool,
                isInstalled = true,
                onAction = {
                    scope.launch {
                        cliToolsManager.uninstall(tool.id)
                        installedTools = cliToolsManager.getInstalledTools()
                        availableTools = cliToolsManager.getAvailableTools()
                    }
                }
            )
        }

        // Available Tools
        items(availableTools) { tool ->
            CLIToolCard(
                tool = tool,
                isInstalled = false,
                onAction = {
                    scope.launch {
                        isInstalling = true
                        cliToolsManager.install(tool.id) { progress, message ->
                            installProgress = progress
                            installMessage = message
                        }.onSuccess {
                            installedTools = cliToolsManager.getInstalledTools()
                            availableTools = cliToolsManager.getAvailableTools()
                            isInstalling = false
                        }.onFailure {
                            installMessage = "エラー: ${it.message}"
                            isInstalling = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CLIToolCard(
    tool: CLITool,
    isInstalled: Boolean,
    onAction: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Download,
                    null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(tool.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "サイズ: ${tool.sizeBytes / 1_000_000}MB",
                    style = MaterialTheme.typography.bodySmall
                )

                if (isInstalled) {
                    OutlinedButton(onClick = onAction) {
                        Text("削除")
                    }
                } else {
                    Button(onClick = onAction) {
                        Text("インストール")
                    }
                }
            }
        }
    }
}
