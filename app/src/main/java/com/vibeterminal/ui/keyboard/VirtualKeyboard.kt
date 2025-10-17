package com.vibeterminal.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Virtual keyboard for terminal with special keys
 * Solves the #1 Termux usability issue: missing Ctrl, Alt, Esc keys
 */
@Composable
fun VirtualKeyboard(
    onKeyPress: (String) -> Unit,
    onSpecialKey: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCtrlPressed by remember { mutableStateOf(false) }
    var isAltPressed by remember { mutableStateOf(false) }
    var isShiftPressed by remember { mutableStateOf(false) }
    var showNumPad by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Quick info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "特殊キー",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isCtrlPressed) ModifierBadge("CTRL")
                    if (isAltPressed) ModifierBadge("ALT")
                    if (isShiftPressed) ModifierBadge("SHIFT")
                }

                IconButton(
                    onClick = { showNumPad = !showNumPad },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (showNumPad) Icons.Default.KeyboardAlt else Icons.Default.Dialpad,
                        contentDescription = "数字パッド切り替え",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider()

            if (showNumPad) {
                // Number pad layout
                NumericKeypad(
                    onKeyPress = { key ->
                        handleKeyPress(key, isCtrlPressed, isAltPressed, isShiftPressed, onKeyPress, onSpecialKey)
                        resetModifiers(
                            ctrl = { isCtrlPressed = false },
                            alt = { isAltPressed = false },
                            shift = { isShiftPressed = false }
                        )
                    }
                )
            } else {
                // Row 1: Modifier keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModifierKey(
                        label = "CTRL",
                        isPressed = isCtrlPressed,
                        onClick = { isCtrlPressed = !isCtrlPressed },
                        modifier = Modifier.weight(1f)
                    )
                    ModifierKey(
                        label = "ALT",
                        isPressed = isAltPressed,
                        onClick = { isAltPressed = !isAltPressed },
                        modifier = Modifier.weight(1f)
                    )
                    ModifierKey(
                        label = "SHIFT",
                        isPressed = isShiftPressed,
                        onClick = { isShiftPressed = !isShiftPressed },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Special function keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpecialKeyButton(
                        label = "ESC",
                        onClick = {
                            onSpecialKey(SpecialKey.ESC)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SpecialKeyButton(
                        label = "TAB",
                        onClick = {
                            onSpecialKey(SpecialKey.TAB)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SpecialKeyButton(
                        label = "ENTER",
                        onClick = {
                            onSpecialKey(SpecialKey.ENTER)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Arrow keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(Modifier.weight(1f))

                    ArrowKeyButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = {
                            onSpecialKey(SpecialKey.ARROW_UP)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        }
                    )

                    Spacer(Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ArrowKeyButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        onClick = {
                            onSpecialKey(SpecialKey.ARROW_LEFT)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        }
                    )
                    ArrowKeyButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = {
                            onSpecialKey(SpecialKey.ARROW_DOWN)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        }
                    )
                    ArrowKeyButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        onClick = {
                            onSpecialKey(SpecialKey.ARROW_RIGHT)
                            resetModifiers(
                                ctrl = { isCtrlPressed = false },
                                alt = { isAltPressed = false },
                                shift = { isShiftPressed = false }
                            )
                        }
                    )
                }

                // Row 4: Common shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShortcutButton(
                        label = "Ctrl+C",
                        subtitle = "中断",
                        onClick = { onSpecialKey(SpecialKey.CTRL_C) },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutButton(
                        label = "Ctrl+D",
                        subtitle = "EOF",
                        onClick = { onSpecialKey(SpecialKey.CTRL_D) },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutButton(
                        label = "Ctrl+Z",
                        subtitle = "停止",
                        onClick = { onSpecialKey(SpecialKey.CTRL_Z) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: Other useful keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpecialKeyButton(
                        label = "|",
                        onClick = { onKeyPress("|") },
                        modifier = Modifier.weight(1f)
                    )
                    SpecialKeyButton(
                        label = "~",
                        onClick = { onKeyPress("~") },
                        modifier = Modifier.weight(1f)
                    )
                    SpecialKeyButton(
                        label = "/",
                        onClick = { onKeyPress("/") },
                        modifier = Modifier.weight(1f)
                    )
                    SpecialKeyButton(
                        label = "-",
                        onClick = { onKeyPress("-") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericKeypad(onKeyPress: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Row 1: F1-F4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..4) {
                SpecialKeyButton(
                    label = "F$i",
                    onClick = { onKeyPress("F$i") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: F5-F8
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 5..8) {
                SpecialKeyButton(
                    label = "F$i",
                    onClick = { onKeyPress("F$i") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: F9-F12
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 9..12) {
                SpecialKeyButton(
                    label = "F$i",
                    onClick = { onKeyPress("F$i") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 4: PageUp, PageDown, Home, End
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialKeyButton(
                label = "PgUp",
                onClick = { onKeyPress("PageUp") },
                modifier = Modifier.weight(1f)
            )
            SpecialKeyButton(
                label = "PgDn",
                onClick = { onKeyPress("PageDown") },
                modifier = Modifier.weight(1f)
            )
            SpecialKeyButton(
                label = "Home",
                onClick = { onKeyPress("Home") },
                modifier = Modifier.weight(1f)
            )
            SpecialKeyButton(
                label = "End",
                onClick = { onKeyPress("End") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModifierKey(
    label: String,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isPressed)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ArrowKeyButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun ShortcutButton(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModifierBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 10.sp
        )
    }
}

private fun handleKeyPress(
    key: String,
    isCtrl: Boolean,
    isAlt: Boolean,
    isShift: Boolean,
    onKeyPress: (String) -> Unit,
    onSpecialKey: (SpecialKey) -> Unit
) {
    // TODO: Implement modifier key combination handling
    onKeyPress(key)
}

private fun resetModifiers(
    ctrl: () -> Unit,
    alt: () -> Unit,
    shift: () -> Unit
) {
    ctrl()
    alt()
    shift()
}

/**
 * Special keys enum
 */
enum class SpecialKey {
    ESC,
    TAB,
    ENTER,
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    CTRL_C,
    CTRL_D,
    CTRL_Z,
    PAGE_UP,
    PAGE_DOWN,
    HOME,
    END
}
