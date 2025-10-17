package com.vibeterminal.ui.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MCP server management
 */
class MCPViewModel : ViewModel() {

    // List of all MCP servers
    private val _servers = MutableStateFlow<List<MCPServer>>(MCPServers.ALL)
    val servers: StateFlow<List<MCPServer>> = _servers.asStateFlow()

    // Currently selected server
    private val _selectedServer = MutableStateFlow<MCPServer?>(null)
    val selectedServer: StateFlow<MCPServer?> = _selectedServer.asStateFlow()

    // Recent MCP tool execution results
    private val _recentResults = MutableStateFlow<List<MCPToolResult>>(emptyList())
    val recentResults: StateFlow<List<MCPToolResult>> = _recentResults.asStateFlow()

    // MCP connection logs
    private val _connectionLogs = MutableStateFlow<List<MCPLogEntry>>(emptyList())
    val connectionLogs: StateFlow<List<MCPLogEntry>> = _connectionLogs.asStateFlow()

    init {
        // Auto-start servers with autoStart = true
        viewModelScope.launch {
            _servers.value.filter { it.autoStart }.forEach { server ->
                startServer(server.id)
            }
        }
    }

    /**
     * Start MCP server
     */
    fun startServer(serverId: String) {
        viewModelScope.launch {
            val server = _servers.value.find { it.id == serverId } ?: return@launch

            // Update status to CONNECTING
            updateServerStatus(serverId, MCPConnectionStatus.CONNECTING)
            addLog(MCPLogEntry(
                timestamp = System.currentTimeMillis(),
                serverName = server.name,
                level = MCPLogLevel.INFO,
                message = "Connecting to ${server.displayName}..."
            ))

            try {
                // Simulate connection (in real implementation, this would start the actual server process)
                delay(1000)

                // Simulate successful connection
                updateServerStatus(serverId, MCPConnectionStatus.CONNECTED)
                updateLastConnected(serverId, System.currentTimeMillis())
                addLog(MCPLogEntry(
                    timestamp = System.currentTimeMillis(),
                    serverName = server.name,
                    level = MCPLogLevel.SUCCESS,
                    message = "✓ Connected to ${server.displayName}"
                ))

            } catch (e: Exception) {
                // Handle connection error
                updateServerStatus(serverId, MCPConnectionStatus.ERROR, e.message)
                addLog(MCPLogEntry(
                    timestamp = System.currentTimeMillis(),
                    serverName = server.name,
                    level = MCPLogLevel.ERROR,
                    message = "✗ Failed to connect: ${e.message}"
                ))
            }
        }
    }

    /**
     * Stop MCP server
     */
    fun stopServer(serverId: String) {
        viewModelScope.launch {
            val server = _servers.value.find { it.id == serverId } ?: return@launch

            updateServerStatus(serverId, MCPConnectionStatus.DISCONNECTED)
            addLog(MCPLogEntry(
                timestamp = System.currentTimeMillis(),
                serverName = server.name,
                level = MCPLogLevel.INFO,
                message = "Disconnected from ${server.displayName}"
            ))
        }
    }

    /**
     * Restart MCP server
     */
    fun restartServer(serverId: String) {
        viewModelScope.launch {
            stopServer(serverId)
            delay(500)
            startServer(serverId)
        }
    }

    /**
     * Execute MCP tool
     */
    fun executeTool(
        serverId: String,
        toolName: String,
        parameters: Map<String, String>,
        onResult: (MCPToolResult) -> Unit
    ) {
        viewModelScope.launch {
            val server = _servers.value.find { it.id == serverId } ?: return@launch
            val tool = server.tools.find { it.name == toolName } ?: return@launch

            val startTime = System.currentTimeMillis()

            addLog(MCPLogEntry(
                timestamp = startTime,
                serverName = server.name,
                level = MCPLogLevel.INFO,
                message = "→ ${tool.displayName}: $parameters"
            ))

            try {
                // Simulate tool execution
                delay(500)

                // Simulate successful result
                val result = MCPToolResult(
                    toolName = toolName,
                    serverName = server.name,
                    success = true,
                    data = "Tool executed successfully with parameters: $parameters",
                    error = null,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )

                addResult(result)
                addLog(MCPLogEntry(
                    timestamp = System.currentTimeMillis(),
                    serverName = server.name,
                    level = MCPLogLevel.SUCCESS,
                    message = "← ${tool.displayName}: Success (${result.executionTimeMs}ms)"
                ))

                onResult(result)

            } catch (e: Exception) {
                val result = MCPToolResult(
                    toolName = toolName,
                    serverName = server.name,
                    success = false,
                    data = null,
                    error = e.message,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )

                addResult(result)
                addLog(MCPLogEntry(
                    timestamp = System.currentTimeMillis(),
                    serverName = server.name,
                    level = MCPLogLevel.ERROR,
                    message = "← ${tool.displayName}: Error - ${e.message}"
                ))

                onResult(result)
            }
        }
    }

    /**
     * Select server for detailed view
     */
    fun selectServer(server: MCPServer?) {
        _selectedServer.value = server
    }

    /**
     * Test server connection
     */
    fun testConnection(serverId: String) {
        viewModelScope.launch {
            val server = _servers.value.find { it.id == serverId } ?: return@launch

            addLog(MCPLogEntry(
                timestamp = System.currentTimeMillis(),
                serverName = server.name,
                level = MCPLogLevel.INFO,
                message = "Testing connection to ${server.displayName}..."
            ))

            // Simulate connection test
            delay(1000)

            val isConnected = server.status == MCPConnectionStatus.CONNECTED

            addLog(MCPLogEntry(
                timestamp = System.currentTimeMillis(),
                serverName = server.name,
                level = if (isConnected) MCPLogLevel.SUCCESS else MCPLogLevel.ERROR,
                message = if (isConnected) "✓ Connection OK" else "✗ Connection failed"
            ))
        }
    }

    /**
     * Clear connection logs
     */
    fun clearLogs() {
        _connectionLogs.value = emptyList()
    }

    /**
     * Clear recent results
     */
    fun clearResults() {
        _recentResults.value = emptyList()
    }

    // Private helper methods

    private fun updateServerStatus(
        serverId: String,
        status: MCPConnectionStatus,
        errorMessage: String? = null
    ) {
        _servers.value = _servers.value.map { server ->
            if (server.id == serverId) {
                server.copy(status = status, errorMessage = errorMessage)
            } else {
                server
            }
        }
    }

    private fun updateLastConnected(serverId: String, timestamp: Long) {
        _servers.value = _servers.value.map { server ->
            if (server.id == serverId) {
                server.copy(lastConnected = timestamp)
            } else {
                server
            }
        }
    }

    private fun addResult(result: MCPToolResult) {
        _recentResults.value = listOf(result) + _recentResults.value.take(19)
    }

    private fun addLog(entry: MCPLogEntry) {
        _connectionLogs.value = listOf(entry) + _connectionLogs.value.take(99)
    }
}

/**
 * MCP connection log entry
 */
data class MCPLogEntry(
    val timestamp: Long,
    val serverName: String,
    val level: MCPLogLevel,
    val message: String
)

/**
 * MCP log level
 */
enum class MCPLogLevel {
    INFO,
    SUCCESS,
    ERROR,
    WARNING
}
