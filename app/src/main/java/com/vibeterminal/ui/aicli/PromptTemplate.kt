package com.vibeterminal.ui.aicli

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Prompt template for AI CLI (Claude Code, Codex, Gemini CLI)
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val category: PromptCategory,
    val template: String,
    val variables: List<PromptVariable> = emptyList(),
    val icon: ImageVector,
    val description: String
) {
    fun render(values: Map<String, String>): String {
        var result = template
        variables.forEach { variable ->
            val value = values[variable.name] ?: variable.defaultValue
            result = result.replace("{{${variable.name}}}", value)
        }
        return result
    }
}

data class PromptVariable(
    val name: String,
    val label: String,
    val placeholder: String,
    val defaultValue: String = ""
)

enum class PromptCategory(val displayName: String) {
    CREATE_PROJECT("プロジェクト作成"),
    FIX_BUG("バグ修正"),
    ADD_FEATURE("機能追加"),
    REFACTOR("リファクタリング"),
    EXPLAIN("コード説明"),
    OPTIMIZE("最適化"),
    TEST("テスト")
}

/**
 * Built-in prompt templates
 */
object PromptTemplates {
    val ALL = listOf(
        // プロジェクト作成
        PromptTemplate(
            id = "create_web_app",
            name = "Webアプリを作成",
            category = PromptCategory.CREATE_PROJECT,
            template = "{{appName}}という{{framework}}のWebアプリを作って。機能は{{features}}。",
            variables = listOf(
                PromptVariable("appName", "アプリ名", "例: TODO管理アプリ"),
                PromptVariable("framework", "フレームワーク", "例: React", "React"),
                PromptVariable("features", "機能", "例: タスクの追加、削除、完了")
            ),
            icon = Icons.Default.Web,
            description = "新しいWebアプリケーションを作成します"
        ),

        PromptTemplate(
            id = "create_api",
            name = "REST APIを作成",
            category = PromptCategory.CREATE_PROJECT,
            template = "{{apiName}}というREST APIを{{language}}で作って。エンドポイントは{{endpoints}}。",
            variables = listOf(
                PromptVariable("apiName", "API名", "例: ユーザー管理API"),
                PromptVariable("language", "言語", "例: Node.js", "Node.js"),
                PromptVariable("endpoints", "エンドポイント", "例: GET /users, POST /users, DELETE /users/:id")
            ),
            icon = Icons.Default.CloudQueue,
            description = "REST APIサーバーを作成します"
        ),

        PromptTemplate(
            id = "create_mobile_app",
            name = "モバイルアプリを作成",
            category = PromptCategory.CREATE_PROJECT,
            template = "{{appName}}という{{platform}}アプリを作って。機能は{{features}}。",
            variables = listOf(
                PromptVariable("appName", "アプリ名", "例: 日記アプリ"),
                PromptVariable("platform", "プラットフォーム", "例: React Native", "React Native"),
                PromptVariable("features", "機能", "例: 日記の記録、画像添付、カレンダー表示")
            ),
            icon = Icons.Default.PhoneAndroid,
            description = "モバイルアプリケーションを作成します"
        ),

        // バグ修正
        PromptTemplate(
            id = "fix_error",
            name = "エラーを修正",
            category = PromptCategory.FIX_BUG,
            template = "以下のエラーを修正して:\n{{errorMessage}}",
            variables = listOf(
                PromptVariable("errorMessage", "エラーメッセージ", "エラーメッセージを貼り付けてください")
            ),
            icon = Icons.Default.BugReport,
            description = "エラーメッセージから問題を特定して修正します"
        ),

        PromptTemplate(
            id = "fix_behavior",
            name = "動作を修正",
            category = PromptCategory.FIX_BUG,
            template = "{{symptom}}という問題が起きています。原因を調べて修正して。",
            variables = listOf(
                PromptVariable("symptom", "症状", "例: ボタンを押しても何も起きない")
            ),
            icon = Icons.Default.Build,
            description = "想定外の動作を修正します"
        ),

        // 機能追加
        PromptTemplate(
            id = "add_feature",
            name = "新機能を追加",
            category = PromptCategory.ADD_FEATURE,
            template = "{{feature}}という機能を追加して。",
            variables = listOf(
                PromptVariable("feature", "機能の説明", "例: ダークモード切り替え機能")
            ),
            icon = Icons.Default.Add,
            description = "新しい機能を追加します"
        ),

        PromptTemplate(
            id = "improve_ui",
            name = "UIを改善",
            category = PromptCategory.ADD_FEATURE,
            template = "{{improvement}}というUIの改善をして。",
            variables = listOf(
                PromptVariable("improvement", "改善内容", "例: ボタンを大きくして、色をもっと見やすく")
            ),
            icon = Icons.Default.Palette,
            description = "ユーザーインターフェースを改善します"
        ),

        // リファクタリング
        PromptTemplate(
            id = "refactor_code",
            name = "コードをリファクタリング",
            category = PromptCategory.REFACTOR,
            template = "{{filePath}}のコードをリファクタリングして、{{goal}}。",
            variables = listOf(
                PromptVariable("filePath", "ファイルパス", "例: src/App.tsx"),
                PromptVariable("goal", "目的", "例: 読みやすくして、重複を減らす", "読みやすくする")
            ),
            icon = Icons.Default.AutoFixHigh,
            description = "コードの品質を向上させます"
        ),

        // コード説明
        PromptTemplate(
            id = "explain_code",
            name = "このコードを説明",
            category = PromptCategory.EXPLAIN,
            template = "{{filePath}}のコードを初心者向けに日本語で説明して。",
            variables = listOf(
                PromptVariable("filePath", "ファイルパス", "例: src/components/Button.tsx")
            ),
            icon = Icons.Default.School,
            description = "コードの動作を分かりやすく説明します"
        ),

        // 最適化
        PromptTemplate(
            id = "optimize_performance",
            name = "パフォーマンスを最適化",
            category = PromptCategory.OPTIMIZE,
            template = "{{target}}のパフォーマンスを最適化して。",
            variables = listOf(
                PromptVariable("target", "対象", "例: 画像の読み込み速度")
            ),
            icon = Icons.Default.Speed,
            description = "アプリケーションの速度を改善します"
        ),

        // テスト
        PromptTemplate(
            id = "add_tests",
            name = "テストを追加",
            category = PromptCategory.TEST,
            template = "{{filePath}}のテストを{{framework}}で書いて。",
            variables = listOf(
                PromptVariable("filePath", "ファイルパス", "例: src/utils/validation.ts"),
                PromptVariable("framework", "テストフレームワーク", "例: Jest", "Jest")
            ),
            icon = Icons.Default.Science,
            description = "自動テストを追加します"
        )
    )

    fun getByCategory(category: PromptCategory): List<PromptTemplate> {
        return ALL.filter { it.category == category }
    }

    fun getById(id: String): PromptTemplate? {
        return ALL.find { it.id == id }
    }
}
