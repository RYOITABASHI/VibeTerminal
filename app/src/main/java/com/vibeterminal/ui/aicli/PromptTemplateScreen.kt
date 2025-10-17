package com.vibeterminal.ui.aicli

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PromptTemplateBar(onTemplateSelected: (String) -> Unit) {
    var showAllTemplates by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "クイックプロンプト",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // クイックテンプレート（最もよく使う3つ）
                QuickTemplateChip(
                    icon = Icons.Default.Web,
                    label = "Web作成",
                    onClick = {
                        val template = PromptTemplates.getById("create_web_app")
                        template?.let { onTemplateSelected(it.template) }
                    }
                )

                QuickTemplateChip(
                    icon = Icons.Default.BugReport,
                    label = "バグ修正",
                    onClick = {
                        val template = PromptTemplates.getById("fix_error")
                        template?.let { onTemplateSelected(it.template) }
                    }
                )

                QuickTemplateChip(
                    icon = Icons.Default.Add,
                    label = "機能追加",
                    onClick = {
                        val template = PromptTemplates.getById("add_feature")
                        template?.let { onTemplateSelected(it.template) }
                    }
                )

                Spacer(Modifier.weight(1f))

                // もっと見るボタン
                IconButton(onClick = { showAllTemplates = true }) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "すべてのテンプレート"
                    )
                }
            }
        }
    }

    if (showAllTemplates) {
        TemplateGalleryDialog(
            onDismiss = { showAllTemplates = false },
            onSelect = { template ->
                showAllTemplates = false
                onTemplateSelected(template.template)
            }
        )
    }
}

@Composable
fun QuickTemplateChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(label)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateGalleryDialog(
    onDismiss: () -> Unit,
    onSelect: (PromptTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PromptCategory?>(null) }
    var selectedTemplate by remember { mutableStateOf<PromptTemplate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column {
                // ヘッダー
                TopAppBar(
                    title = { Text("プロンプトテンプレート") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "閉じる")
                        }
                    }
                )

                // カテゴリタブ
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory?.ordinal ?: -1
                ) {
                    Tab(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        text = { Text("すべて") }
                    )
                    PromptCategory.values().forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = { Text(category.displayName) }
                        )
                    }
                }

                // テンプレートリスト
                val filteredTemplates = if (selectedCategory == null) {
                    PromptTemplates.ALL
                } else {
                    PromptTemplates.getByCategory(selectedCategory!!)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTemplates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }
            }
        }
    }

    // テンプレート詳細ダイアログ
    selectedTemplate?.let { template ->
        TemplateDetailDialog(
            template = template,
            onDismiss = { selectedTemplate = null },
            onApply = { renderedPrompt ->
                selectedTemplate = null
                onSelect(template)
            }
        )
    }
}

@Composable
fun TemplateCard(
    template: PromptTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                template.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
fun TemplateDetailDialog(
    template: PromptTemplate,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var variableValues by remember {
        mutableStateOf(
            template.variables.associate { it.name to it.defaultValue }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // ヘッダー
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        template.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        template.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // 変数入力フォーム
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(template.variables) { variable ->
                        OutlinedTextField(
                            value = variableValues[variable.name] ?: "",
                            onValueChange = { value ->
                                variableValues = variableValues + (variable.name to value)
                            },
                            label = { Text(variable.label) },
                            placeholder = { Text(variable.placeholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = variable.name != "features" && variable.name != "errorMessage"
                        )
                    }

                    // プレビュー
                    item {
                        Text(
                            "プロンプトプレビュー:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                template.render(variableValues),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // アクションボタン
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
                            val renderedPrompt = template.render(variableValues)
                            onApply(renderedPrompt)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("適用")
                    }
                }
            }
        }
    }
}
