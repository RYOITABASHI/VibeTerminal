package com.vibeterminal.ui.mcp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * MCP Server connection status
 */
enum class MCPConnectionStatus {
    CONNECTED,      // 接続中
    DISCONNECTED,   // 切断
    CONNECTING,     // 接続試行中
    ERROR          // エラー
}

/**
 * MCP Server configuration
 */
data class MCPServer(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val command: String,
    val args: List<String> = emptyList(),
    val port: Int? = null,
    val autoStart: Boolean = false,
    val status: MCPConnectionStatus = MCPConnectionStatus.DISCONNECTED,
    val tools: List<MCPTool> = emptyList(),
    val errorMessage: String? = null,
    val lastConnected: Long? = null
)

/**
 * MCP Tool provided by a server
 */
data class MCPTool(
    val name: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val parameters: List<MCPToolParameter> = emptyList(),
    val category: MCPToolCategory
)

/**
 * MCP Tool parameter
 */
data class MCPToolParameter(
    val name: String,
    val displayName: String,
    val type: String,
    val required: Boolean = true,
    val defaultValue: String? = null,
    val placeholder: String? = null
)

/**
 * MCP Tool category
 */
enum class MCPToolCategory(val displayName: String) {
    FILESYSTEM("ファイルシステム"),
    BROWSER("ブラウザ"),
    API("API"),
    DATABASE("データベース"),
    AI("AI"),
    OTHER("その他")
}

/**
 * MCP Tool execution result
 */
data class MCPToolResult(
    val toolName: String,
    val serverName: String,
    val success: Boolean,
    val data: String?,
    val error: String?,
    val executionTimeMs: Long
)

/**
 * Built-in MCP server configurations
 */
object MCPServers {
    val SERENA = MCPServer(
        id = "serena",
        name = "serena",
        displayName = "Serena",
        description = "ファイルシステムとコード検索",
        icon = Icons.Default.Folder,
        command = "serena",
        args = listOf(),
        autoStart = true,
        tools = listOf(
            MCPTool(
                name = "list_files",
                displayName = "ファイル一覧",
                description = "ディレクトリ内のファイルを一覧表示",
                icon = Icons.Default.List,
                parameters = listOf(
                    MCPToolParameter("path", "パス", "string", required = false, placeholder = ".")
                ),
                category = MCPToolCategory.FILESYSTEM
            ),
            MCPTool(
                name = "read_file",
                displayName = "ファイル読み込み",
                description = "ファイルの内容を読み込む",
                icon = Icons.Default.FileOpen,
                parameters = listOf(
                    MCPToolParameter("path", "ファイルパス", "string", required = true)
                ),
                category = MCPToolCategory.FILESYSTEM
            ),
            MCPTool(
                name = "search_code",
                displayName = "コード検索",
                description = "プロジェクト内のコードを検索",
                icon = Icons.Default.Search,
                parameters = listOf(
                    MCPToolParameter("query", "検索キーワード", "string", required = true),
                    MCPToolParameter("path", "検索パス", "string", required = false, placeholder = ".")
                ),
                category = MCPToolCategory.FILESYSTEM
            ),
            MCPTool(
                name = "write_file",
                displayName = "ファイル書き込み",
                description = "ファイルに内容を書き込む",
                icon = Icons.Default.Edit,
                parameters = listOf(
                    MCPToolParameter("path", "ファイルパス", "string", required = true),
                    MCPToolParameter("content", "内容", "string", required = true)
                ),
                category = MCPToolCategory.FILESYSTEM
            )
        )
    )

    val PLAYWRIGHT = MCPServer(
        id = "playwright",
        name = "playwright",
        displayName = "Playwright",
        description = "ブラウザ自動化とWebスクレイピング",
        icon = Icons.Default.Web,
        command = "npx",
        args = listOf("@modelcontextprotocol/server-playwright"),
        port = 3000,
        tools = listOf(
            MCPTool(
                name = "navigate",
                displayName = "ページ遷移",
                description = "指定したURLに移動",
                icon = Icons.Default.Navigation,
                parameters = listOf(
                    MCPToolParameter("url", "URL", "string", required = true, placeholder = "https://example.com")
                ),
                category = MCPToolCategory.BROWSER
            ),
            MCPTool(
                name = "screenshot",
                displayName = "スクリーンショット",
                description = "現在のページのスクリーンショットを取得",
                icon = Icons.Default.CameraAlt,
                parameters = listOf(
                    MCPToolParameter("url", "URL", "string", required = true),
                    MCPToolParameter("fullPage", "全画面", "boolean", required = false, defaultValue = "false")
                ),
                category = MCPToolCategory.BROWSER
            ),
            MCPTool(
                name = "click",
                displayName = "要素クリック",
                description = "ページ上の要素をクリック",
                icon = Icons.Default.TouchApp,
                parameters = listOf(
                    MCPToolParameter("selector", "セレクタ", "string", required = true, placeholder = "#button-id")
                ),
                category = MCPToolCategory.BROWSER
            ),
            MCPTool(
                name = "fill",
                displayName = "フォーム入力",
                description = "フォームに値を入力",
                icon = Icons.Default.Input,
                parameters = listOf(
                    MCPToolParameter("selector", "セレクタ", "string", required = true),
                    MCPToolParameter("value", "入力値", "string", required = true)
                ),
                category = MCPToolCategory.BROWSER
            ),
            MCPTool(
                name = "get_content",
                displayName = "HTML取得",
                description = "ページのHTMLコンテンツを取得",
                icon = Icons.Default.Code,
                parameters = listOf(
                    MCPToolParameter("url", "URL", "string", required = true)
                ),
                category = MCPToolCategory.BROWSER
            )
        )
    )

    val GITHUB = MCPServer(
        id = "github",
        name = "github",
        displayName = "GitHub",
        description = "GitHubリポジトリ操作",
        icon = Icons.Default.Cloud,
        command = "npx",
        args = listOf("@modelcontextprotocol/server-github"),
        tools = listOf(
            MCPTool(
                name = "create_pr",
                displayName = "PR作成",
                description = "プルリクエストを作成",
                icon = Icons.Default.MergeType,
                parameters = listOf(
                    MCPToolParameter("title", "タイトル", "string", required = true),
                    MCPToolParameter("body", "説明", "string", required = false),
                    MCPToolParameter("base", "ベースブランチ", "string", required = false, defaultValue = "main")
                ),
                category = MCPToolCategory.API
            ),
            MCPTool(
                name = "list_issues",
                displayName = "Issue一覧",
                description = "Issueの一覧を取得",
                icon = Icons.Default.BugReport,
                parameters = listOf(
                    MCPToolParameter("state", "状態", "string", required = false, defaultValue = "open")
                ),
                category = MCPToolCategory.API
            ),
            MCPTool(
                name = "create_issue",
                displayName = "Issue作成",
                description = "新しいIssueを作成",
                icon = Icons.Default.AddCircle,
                parameters = listOf(
                    MCPToolParameter("title", "タイトル", "string", required = true),
                    MCPToolParameter("body", "内容", "string", required = false)
                ),
                category = MCPToolCategory.API
            )
        )
    )

    val ALL = listOf(SERENA, PLAYWRIGHT, GITHUB)

    fun getById(id: String): MCPServer? {
        return ALL.find { it.id == id }
    }

    fun getByCategory(category: MCPToolCategory): List<MCPTool> {
        return ALL.flatMap { server ->
            server.tools.filter { it.category == category }
        }
    }
}
