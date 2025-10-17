package com.vibeterminal.core.translator

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Translation Engine - Core component for real-time command output translation
 *
 * Features:
 * - Local pattern matching (fast, offline)
 * - LLM API fallback (accurate, online)
 * - Smart caching
 * - Multi-language support (Japanese first)
 */

@Serializable
data class TranslationPattern(
    val regex: String,
    val translation: String,
    val emoji: String,
    val category: String, // info, success, warning, error, progress
    val suggestion: String? = null
)

@Serializable
data class TranslationDatabase(
    val patterns: List<TranslationPattern>,
    val commands: Map<String, CommandInfo>? = null
)

@Serializable
data class CommandInfo(
    val description: String,
    val common_errors: List<String>? = null
)

data class TranslatedOutput(
    val originalText: String,
    val translatedText: String,
    val emoji: String?,
    val category: String,
    val suggestion: String? = null,
    val confidence: Float,
    val source: TranslationSource
)

enum class TranslationSource {
    LOCAL_PATTERN,
    LLM_API,
    CACHE
}

class TranslationEngine(
    private val patternsDir: File,
    private val llmApiKey: String? = null
) {
    private val patterns = mutableListOf<Pair<Regex, TranslationPattern>>()
    private val cache = TranslationCache()
    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadPatterns()
    }

    /**
     * Load translation patterns from JSON files
     */
    private fun loadPatterns() {
        val patternFiles = listOf(
            "git.json",
            "npm.json",
            "docker.json",
            "common.json"
        )

        patternFiles.forEach { filename ->
            val file = File(patternsDir, filename)
            if (file.exists()) {
                try {
                    val db = json.decodeFromString<TranslationDatabase>(file.readText())
                    db.patterns.forEach { pattern ->
                        val regex = Regex(pattern.regex, RegexOption.MULTILINE)
                        patterns.add(regex to pattern)
                    }
                } catch (e: Exception) {
                    // Log error but continue
                    println("Failed to load pattern file: $filename - ${e.message}")
                }
            }
        }
    }

    /**
     * Translate command output
     *
     * @param command The command that was executed
     * @param output The command's output
     * @param useLLM Whether to use LLM API if local match fails
     * @return Translated output
     */
    suspend fun translate(
        command: String,
        output: String,
        useLLM: Boolean = true
    ): TranslatedOutput = withContext(Dispatchers.Default) {

        // 1. Check cache
        cache.get(command, output)?.let {
            return@withContext it.copy(source = TranslationSource.CACHE)
        }

        // 2. Try local pattern matching
        val localResult = translateLocally(output)
        if (localResult.confidence > 0.7f) {
            cache.put(command, output, localResult)
            return@withContext localResult
        }

        // 3. Fallback to LLM API (if enabled and API key available)
        if (useLLM && llmApiKey != null) {
            try {
                val llmResult = translateWithLLM(command, output)
                cache.put(command, output, llmResult)
                return@withContext llmResult
            } catch (e: Exception) {
                // If LLM fails, return local result
                println("LLM translation failed: ${e.message}")
            }
        }

        // 4. Return local result as fallback
        return@withContext localResult
    }

    /**
     * Translate using local patterns
     */
    private fun translateLocally(output: String): TranslatedOutput {
        val lines = output.lines()
        val translatedLines = mutableListOf<String>()
        var bestMatch: TranslationPattern? = null
        var matchCount = 0

        lines.forEach { line ->
            var matched = false

            // Try to match each pattern
            for ((regex, pattern) in patterns) {
                val matchResult = regex.find(line)
                if (matchResult != null) {
                    // Replace capture groups
                    var translated = pattern.translation
                    matchResult.groupValues.forEachIndexed { index, value ->
                        if (index > 0) { // Skip group 0 (entire match)
                            translated = translated.replace("$$index", value)
                        }
                    }

                    translatedLines.add("${pattern.emoji} $translated")
                    matched = true
                    matchCount++

                    // Keep track of the best match (for metadata)
                    if (bestMatch == null || pattern.category == "error" || pattern.category == "warning") {
                        bestMatch = pattern
                    }

                    break
                }
            }

            if (!matched && line.isNotBlank()) {
                // No pattern matched, keep original
                translatedLines.add(line)
            }
        }

        val totalLines = lines.filter { it.isNotBlank() }.size
        val confidence = if (totalLines > 0) matchCount.toFloat() / totalLines else 0f

        return TranslatedOutput(
            originalText = output,
            translatedText = translatedLines.joinToString("\n"),
            emoji = bestMatch?.emoji,
            category = bestMatch?.category ?: "info",
            suggestion = bestMatch?.suggestion,
            confidence = confidence,
            source = TranslationSource.LOCAL_PATTERN
        )
    }

    /**
     * Translate using LLM API (Claude, GPT, etc.)
     */
    private suspend fun translateWithLLM(
        command: String,
        output: String
    ): TranslatedOutput = withContext(Dispatchers.IO) {

        // TODO: Implement actual LLM API call
        // For now, return a placeholder

        val prompt = """
        コマンド: $command
        出力:
        $output

        上記のコマンド出力を日本語で簡潔に説明してください。
        - 絵文字を使ってわかりやすく
        - エラーの場合は解決方法も提示
        - 1-3行程度で簡潔に
        """.trimIndent()

        // Placeholder - actual implementation will call Claude/GPT API
        return@withContext TranslatedOutput(
            originalText = output,
            translatedText = "🤖 LLM翻訳: (実装予定)",
            emoji = "🤖",
            category = "info",
            suggestion = null,
            confidence = 0.9f,
            source = TranslationSource.LLM_API
        )
    }

    /**
     * Get explanation for a specific command
     */
    fun explainCommand(command: String): String? {
        // Parse command to get base command (e.g., "git push" from "git push origin main")
        val baseCommand = command.trim().split(" ").take(2).joinToString(" ")

        // TODO: Load from commands database
        return when (baseCommand) {
            "git push" -> "ローカルの変更をリモートリポジトリに送信します"
            "git pull" -> "リモートの変更を取得してマージします"
            "npm install" -> "package.jsonの依存関係をインストールします"
            else -> null
        }
    }
}

/**
 * Translation cache for performance
 */
class TranslationCache {
    private val cache = mutableMapOf<String, TranslatedOutput>()
    private val maxSize = 1000

    fun get(command: String, output: String): TranslatedOutput? {
        val key = "${command}:${output.hashCode()}"
        return cache[key]
    }

    fun put(command: String, output: String, result: TranslatedOutput) {
        val key = "${command}:${output.hashCode()}"

        // Simple LRU: if cache is full, remove oldest entries
        if (cache.size >= maxSize) {
            val toRemove = cache.keys.take(maxSize / 4)
            toRemove.forEach { cache.remove(it) }
        }

        cache[key] = result
    }

    fun clear() {
        cache.clear()
    }
}
