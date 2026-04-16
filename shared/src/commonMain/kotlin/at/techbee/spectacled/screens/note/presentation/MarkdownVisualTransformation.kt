package at.techbee.spectacled.screens.note.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation(val localContentColor: Color) : VisualTransformation {

    val tagAlpha = 0.33f

    override fun filter(text: AnnotatedString): TransformedText {


        return TransformedText(formatAnnotatedString(text), OffsetMapping.Identity)
    }

    fun formatAnnotatedString(text: String?) = formatAnnotatedString(androidx.compose.ui.text.AnnotatedString(text ?: ""))

    fun formatAnnotatedString(text: AnnotatedString): AnnotatedString {

        val input = text.text
        val builder = AnnotatedString.Builder()

        var i = 0

        while (i < input.length) {
            if (input[i] == '*' || input[i] == '_') {
                val delimiter = input[i]
                val start = i

                // Find closing delimiter
                val end = input.indexOf(delimiter, start + 1)

                if (end != -1) {
                    // Apply faded style to opening delimiter
                    builder.pushStyle(
                        SpanStyle(localContentColor.copy(alpha = tagAlpha))
                    )
                    builder.append(delimiter)
                    builder.pop()

                    // Apply formatting to inner text
                    val content = input.substring(start + 1, end)

                    val style = when (delimiter) {
                        '*' -> SpanStyle(fontWeight = FontWeight.Bold)
                        '_' -> SpanStyle(fontStyle = FontStyle.Italic)
                        else -> SpanStyle()
                    }

                    builder.pushStyle(style)
                    builder.append(content)
                    builder.pop()

                    // Apply faded style to closing delimiter
                    builder.pushStyle(
                        SpanStyle(localContentColor.copy(alpha = tagAlpha))
                    )
                    builder.append(delimiter)
                    builder.pop()

                    i = end + 1
                    continue
                }
            }

            builder.append(input[i])
            i++
        }
        return builder.toAnnotatedString()
    }
}