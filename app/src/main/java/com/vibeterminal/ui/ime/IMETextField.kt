package com.vibeterminal.ui.ime

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * Compose wrapper for JapaneseIMEBridge
 * Provides a text field with Japanese IME support
 */
@Composable
fun JapaneseIMEBridge(
    value: String,
    onValueChange: (String) -> Unit,
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = Color(0xFFD4D4D4)
        ),
        cursorBrush = SolidColor(Color(0xFF4EC9B0)),
        singleLine = false,
        maxLines = 5
    )
}
