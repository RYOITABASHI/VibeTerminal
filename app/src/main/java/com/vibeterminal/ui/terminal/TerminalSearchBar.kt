package com.vibeterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Search bar for terminal output
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<SearchResult>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search icon
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF808080)
            )

            // Search input
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "ターミナル内を検索...",
                        style = TextStyle(fontSize = 12.sp),
                        color = Color(0xFF606060)
                    )
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Result counter
            if (searchResults.isNotEmpty()) {
                Text(
                    "${currentIndex + 1}/${searchResults.size}",
                    style = TextStyle(fontSize = 11.sp),
                    color = Color(0xFF808080)
                )

                // Previous button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "前へ",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }

                // Next button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "次へ",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "閉じる",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF808080)
                )
            }
        }
    }
}
