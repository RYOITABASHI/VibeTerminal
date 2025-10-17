package com.vibeterminal.ui.ime

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.KeyEvent

/**
 * Japanese IME Bridge
 *
 * Enables real-time Japanese input in terminal by intercepting
 * composing text and displaying it before finalization.
 *
 * Problem: Standard Android IME only sends text after user confirms.
 * Solution: Capture composing text and display it in real-time.
 */

class JapaneseIMEBridge(
    target: InputConnection,
    private val onComposingTextUpdate: (CharSequence) -> Unit,
    private val onTextCommit: (CharSequence) -> Unit
) : InputConnectionWrapper(target, true) {

    private val composingText = StringBuilder()
    private var isComposing = false

    /**
     * Called when user types but hasn't confirmed yet
     * This is where we capture Japanese input in real-time
     */
    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        isComposing = true

        if (text != null) {
            composingText.clear()
            composingText.append(text)

            // Notify UI to show composing text
            onComposingTextUpdate(composingText)
        }

        return super.setComposingText(text, newCursorPosition)
    }

    /**
     * Called when user confirms the text (e.g., presses space or Enter)
     */
    override fun finishComposingText(): Boolean {
        if (isComposing && composingText.isNotEmpty()) {
            // Commit the final text to terminal
            onTextCommit(composingText.toString())
            composingText.clear()
        }

        isComposing = false
        return super.finishComposingText()
    }

    /**
     * Called when user commits text directly
     */
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text != null) {
            onTextCommit(text)
        }

        composingText.clear()
        isComposing = false

        return super.commitText(text, newCursorPosition)
    }

    /**
     * Handle backspace during composition
     */
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (isComposing && composingText.isNotEmpty()) {
            // Update composing text
            val newLength = (composingText.length - beforeLength).coerceAtLeast(0)
            composingText.setLength(newLength)
            onComposingTextUpdate(composingText)
        }

        return super.deleteSurroundingText(beforeLength, afterLength)
    }

    /**
     * Handle key events (for physical keyboard)
     */
    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        // Let physical keyboard events pass through normally
        return super.sendKeyEvent(event)
    }

    companion object {
        /**
         * Create EditorInfo optimized for terminal input
         */
        fun createTerminalEditorInfo(): EditorInfo {
            return EditorInfo().apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                           android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                           android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

                imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN or
                            android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI

                // Allow multiline input
                initialSelStart = 0
                initialSelEnd = 0
            }
        }
    }
}

/**
 * Composing text overlay for visual feedback
 */
data class ComposingTextState(
    val text: String = "",
    val isActive: Boolean = false,
    val cursorPosition: Int = 0
)
