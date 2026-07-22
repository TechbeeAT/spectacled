package at.techbee.spectacled.screens.core.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The inline formatting options supported by [MarkdownVisualTransformation].
 *
 * Each option is a single delimiter character that both opens and closes a span, e.g. `*bold*`.
 * Keeping the delimiters here means the formatter (this file) and the renderer
 * ([MarkdownVisualTransformation]) share a single source of truth.
 */
enum class MarkdownFormat(val tag: Char) {
    BOLD('*'),
    ITALIC('_'),
    UNDERLINE('~')
}

/**
 * Applies [format] to this [TextFieldValue] and returns the new value, ready to be assigned back to
 * the editor's state. Intended to be called from a formatting button.
 *
 * - If text is selected, the selection is wrapped with the format's delimiter on each side and stays
 *   selected afterwards.
 * - If nothing is selected, an empty delimiter pair is inserted at the caret and the caret is placed
 *   between the two delimiters, ready for the user to type.
 */
fun TextFieldValue.applyMarkdownFormat(format: MarkdownFormat): TextFieldValue {
    val tag = format.tag

    return if (selection.collapsed) {
        // No selection: insert an empty tag pair and place the caret between the tags.
        val at = selection.start
        val newText = buildString {
            append(text, 0, at)
            append(tag)
            append(tag)
            append(text, at, text.length)
        }
        copy(text = newText, selection = TextRange(at + 1))
    } else {
        // Selection: wrap it and keep the same text selected (shifted by the opening delimiter).
        val start = selection.min
        val end = selection.max
        val newText = buildString {
            append(text, 0, start)
            append(tag)
            append(text, start, end)
            append(tag)
            append(text, end, text.length)
        }
        copy(text = newText, selection = TextRange(start + 1, end + 1))
    }
}

/** Wraps the selection (or inserts a tag pair at the caret) with the bold delimiter. */
fun TextFieldValue.formatBold(): TextFieldValue = applyMarkdownFormat(MarkdownFormat.BOLD)

/** Wraps the selection (or inserts a tag pair at the caret) with the italic delimiter. */
fun TextFieldValue.formatItalic(): TextFieldValue = applyMarkdownFormat(MarkdownFormat.ITALIC)

/** Wraps the selection (or inserts a tag pair at the caret) with the underline delimiter. */
fun TextFieldValue.formatUnderline(): TextFieldValue = applyMarkdownFormat(MarkdownFormat.UNDERLINE)
