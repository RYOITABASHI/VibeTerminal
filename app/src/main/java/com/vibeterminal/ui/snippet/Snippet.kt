package com.vibeterminal.ui.snippet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

/**
 * Snippet/Macro panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetPanel(
    snippets: List<Snippet>,
    onSnippetClick: (Snippet) -> Unit,
    onSnippetDelete: (Snippet) -> Unit,
    onSnippetEdit: (Snippet) -> Unit,
    onAddSnippet: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(350.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF4EC9B0)
                    )
                    Text(
                        "スニペット",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onAddSnippet,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "追加",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF4EC9B0)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "閉じる",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF808080)
                        )
                    }
                }
            }

            Divider(color = Color(0xFF3D3D3D))

            // Snippet grid
            if (snippets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF505050)
                        )
                        Text(
                            "スニペットなし",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF808080)
                        )
                        Button(
                            onClick = onAddSnippet,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4EC9B0)
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("スニペット追加")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snippets) { snippet ->
                        SnippetCard(
                            snippet = snippet,
                            onClick = { onSnippetClick(snippet) },
                            onEdit = { onSnippetEdit(snippet) },
                            onDelete = { onSnippetDelete(snippet) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnippetCard(
    snippet: Snippet,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2D2D2D)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        snippet.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        snippet.command,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF909090),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snippet.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3D3D3D)
                        ) {
                            Text(
                                snippet.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }
            }

            // Menu button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "メニュー",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF808080)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("編集") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("削除") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Snippet data class
 */
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val command: String,
    val description: String = "",
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Default snippets
 */
object DefaultSnippets {
    val ALL = listOf(
        Snippet(
            title = "Git Status",
            command = "git status",
            category = "Git"
        ),
        Snippet(
            title = "Git Push",
            command = "git add . && git commit -m \"update\" && git push",
            category = "Git"
        ),
        Snippet(
            title = "NPM Install",
            command = "npm install",
            category = "NPM"
        ),
        Snippet(
            title = "NPM Start",
            command = "npm start",
            category = "NPM"
        ),
        Snippet(
            title = "List Files",
            command = "ls -lah",
            category = "File"
        ),
        Snippet(
            title = "Disk Usage",
            command = "df -h",
            category = "System"
        ),
        Snippet(
            title = "Find Process",
            command = "ps aux | grep ",
            category = "System"
        ),
        Snippet(
            title = "Network Status",
            command = "ifconfig",
            category = "Network"
        )
    )
}
