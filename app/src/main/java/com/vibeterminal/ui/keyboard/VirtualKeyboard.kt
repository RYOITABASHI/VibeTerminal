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
 * Virtual keyboard for terminal with special keys (compact, collapsible)
 * Solves the #1 Termux usability issue: missing Ctrl, Alt, Esc keys
 */
@Composable
fun VirtualKeyboard(
    onKeyPress: (String) -> Unit,
    onSpecialKey: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isCtrlPressed by remember { mutableStateOf(false) }
    var isAltPressed by remember { mutableStateOf(false) }
    var isShiftPressed by remember { mutableStateOf(false) }
    var showNumPad by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
    ) {
        // Compact header bar (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle button
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "折りたたむ" else "展開",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF4EC9B0)
                )
            }

            // Compact key row (always visible)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactKeyButton("ESC") { onSpecialKey(SpecialKey.ESC) }
                CompactKeyButton("TAB") { onSpecialKey(SpecialKey.TAB) }
                CompactKeyButton("C^C") { onSpecialKey(SpecialKey.CTRL_C) }
                CompactKeyButton("C^D") { onSpecialKey(SpecialKey.CTRL_D) }
            }

            // NumPad toggle
            if (isExpanded) {
                IconButton(
                    onClick = { showNumPad = !showNumPad },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (showNumPad) Icons.Default.KeyboardAlt else Icons.Default.Dialpad,
                        contentDescription = "数字パッド",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF808080)
                    )
                }
            }
        }

        // Expanded keyboard (collapsible)
        if (isExpanded) {
            Divider(color = Color(0xFF1A1A1A))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 90.dp)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
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
                    // Row 1: Modifier keys (compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                    // Row 2: Special function keys (compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                    // Row 3: Arrow keys (compact horizontal layout)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ArrowKeyButton(icon = Icons.Default.KeyboardArrowLeft,
                            onClick = { onSpecialKey(SpecialKey.ARROW_LEFT); resetModifiers({ isCtrlPressed = false }, { isAltPressed = false }, { isShiftPressed = false }) })
                        ArrowKeyButton(icon = Icons.Default.KeyboardArrowUp,
                            onClick = { onSpecialKey(SpecialKey.ARROW_UP); resetModifiers({ isCtrlPressed = false }, { isAltPressed = false }, { isShiftPressed = false }) })
                        ArrowKeyButton(icon = Icons.Default.KeyboardArrowDown,
                            onClick = { onSpecialKey(SpecialKey.ARROW_DOWN); resetModifiers({ isCtrlPressed = false }, { isAltPressed = false }, { isShiftPressed = false }) })
                        ArrowKeyButton(icon = Icons.Default.KeyboardArrowRight,
                            onClick = { onSpecialKey(SpecialKey.ARROW_RIGHT); resetModifiers({ isCtrlPressed = false }, { isAltPressed = false }, { isShiftPressed = false }) })
                    }

                    // Row 4: Special characters (compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SpecialKeyButton(label = "|", onClick = { onKeyPress("|") }, modifier = Modifier.weight(1f))
                        SpecialKeyButton(label = "~", onClick = { onKeyPress("~") }, modifier = Modifier.weight(1f))
                        SpecialKeyButton(label = "/", onClick = { onKeyPress("/") }, modifier = Modifier.weight(1f))
                        SpecialKeyButton(label = "-", onClick = { onKeyPress("-") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Compact key button for header bar
@Composable
private fun CompactKeyButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(24.dp)
            .widthIn(min = 40.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0A0A0A),
            contentColor = Color(0xFF808080)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3A3A3A))
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
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
        modifier = modifier.height(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) Color(0xFF4EC9B0) else Color(0xFF0A0A0A),
            contentColor = if (isPressed) Color(0xFF000000) else Color(0xFF808080)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
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
        modifier = modifier.height(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0A0A0A),
            contentColor = Color(0xFF808080)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3A3A3A)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}

@Composable
private fun ArrowKeyButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(24.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0A0A0A),
            contentColor = Color(0xFF808080)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3A3A3A))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
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
