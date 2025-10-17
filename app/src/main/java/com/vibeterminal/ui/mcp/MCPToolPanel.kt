package com.vibeterminal.ui.mcp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * MCP Tool Quick Access Panel
 * Shows tools from all connected servers
 */
@Composable
fun MCPToolQuickAccess(
    viewModel: MCPViewModel,
    onToolSelected: (MCPServer, MCPTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()
    val connectedServers = servers.filter { it.status == MCPConnectionStatus.CONNECTED }

    if (connectedServers.isEmpty()) {
        return
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "クイックツール",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                connectedServers.forEach { server ->
                    // Show first 3 tools from each server
                    items(server.tools.take(3)) { tool ->
                        MCPToolChip(
                            server = server,
                            tool = tool,
                            onClick = { onToolSelected(server, tool) }
                        )
                    }
                }

                // "More" button
                item {
                    var showAllTools by remember { mutableStateOf(false) }

                    FilterChip(
                        selected = false,
                        onClick = { showAllTools = true },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("すべて")
                            }
                        }
                    )

                    if (showAllTools) {
                        MCPToolGalleryDialog(
                            servers = connectedServers,
                            onDismiss = { showAllTools = false },
                            onSelect = { server, tool ->
                                showAllTools = false
                                onToolSelected(server, tool)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MCPToolChip(
    server: MCPServer,
    tool: MCPTool,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(tool.displayName)
            }
        },
        leadingIcon = {
            Icon(
                server.icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * MCP Tool Gallery Dialog
 * Shows all tools from all servers organized by category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPToolGalleryDialog(
    servers: List<MCPServer>,
    onDismiss: () -> Unit,
    onSelect: (MCPServer, MCPTool) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<MCPToolCategory?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column {
                // Header
                TopAppBar(
                    title = { Text("MCPツール") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "閉じる")
                        }
                    }
                )

                // Category tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory?.ordinal ?: -1
                ) {
                    Tab(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        text = { Text("すべて") }
                    )
                    MCPToolCategory.values().forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = { Text(category.displayName) }
                        )
                    }
                }

                // Tools list grouped by server
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    servers.forEach { server ->
                        val filteredTools = if (selectedCategory == null) {
                            server.tools
                        } else {
                            server.tools.filter { it.category == selectedCategory }
                        }

                        if (filteredTools.isNotEmpty()) {
                            item {
                                MCPServerToolGroup(
                                    server = server,
                                    tools = filteredTools,
                                    onToolClick = { tool -> onSelect(server, tool) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MCPServerToolGroup(
    server: MCPServer,
    tools: List<MCPTool>,
    onToolClick: (MCPTool) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Server header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                server.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                server.displayName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "(${tools.size})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tools
        tools.forEach { tool ->
            MCPToolCard(
                tool = tool,
                onClick = { onToolClick(tool) }
            )
        }
    }
}

@Composable
private fun MCPToolCard(
    tool: MCPTool,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                tool.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tool.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tool.parameters.isNotEmpty()) {
                    Text(
                        "${tool.parameters.size} parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

/**
 * MCP Tool Execution Dialog
 * Shows parameter input form and executes the tool
 */
@Composable
fun MCPToolExecutionDialog(
    server: MCPServer,
    tool: MCPTool,
    viewModel: MCPViewModel,
    onDismiss: () -> Unit,
    onCommandGenerated: (String) -> Unit
) {
    var parameterValues by remember {
        mutableStateOf(
            tool.parameters.associate { it.name to (it.defaultValue ?: "") }
        )
    }
    var isExecuting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<MCPToolResult?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        tool.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            tool.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            server.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Parameters
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (tool.parameters.isEmpty()) {
                        item {
                            Text(
                                "このツールにパラメータはありません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(tool.parameters) { parameter ->
                            OutlinedTextField(
                                value = parameterValues[parameter.name] ?: "",
                                onValueChange = { value ->
                                    parameterValues = parameterValues + (parameter.name to value)
                                },
                                label = {
                                    Text(
                                        parameter.displayName + if (parameter.required) " *" else ""
                                    )
                                },
                                placeholder = {
                                    parameter.placeholder?.let { Text(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = parameter.type != "text"
                            )
                        }
                    }

                    // Result display
                    result?.let { res ->
                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (res.success)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null
                                        )
                                        Text(
                                            if (res.success) "実行成功" else "実行エラー",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        res.data ?: res.error ?: "Unknown result",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "実行時間: ${res.executionTimeMs}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("キャンセル")
                    }
                    Button(
                        onClick = {
                            isExecuting = true
                            viewModel.executeTool(
                                serverId = server.id,
                                toolName = tool.name,
                                parameters = parameterValues
                            ) { res ->
                                isExecuting = false
                                result = res

                                // Generate command for AI CLI
                                val command = generateAICommand(server, tool, parameterValues)
                                onCommandGenerated(command)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExecuting && validateParameters(tool, parameterValues)
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("実行")
                    }
                }
            }
        }
    }
}

// Helper functions

private fun validateParameters(
    tool: MCPTool,
    values: Map<String, String>
): Boolean {
    return tool.parameters
        .filter { it.required }
        .all { param -> values[param.name]?.isNotBlank() == true }
}

private fun generateAICommand(
    server: MCPServer,
    tool: MCPTool,
    parameters: Map<String, String>
): String {
    val params = parameters
        .filter { it.value.isNotBlank() }
        .map { "${it.key}=\"${it.value}\"" }
        .joinToString(" ")

    return "Use ${server.name}.${tool.name} with $params"
}
