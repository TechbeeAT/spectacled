package at.techbee.spectacled.screens.core.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

class MarkdownVisualTransformation(
    val localContentColor: Color,
    val linkColor: Color = Color(0xFF2196F3)
) : VisualTransformation {

    val tagAlpha = 0.33f

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(formatAnnotatedString(text), OffsetMapping.Identity)
    }

    fun formatAnnotatedString(text: String?) = formatAnnotatedString(AnnotatedString(text ?: ""))

    fun formatAnnotatedString(text: AnnotatedString): AnnotatedString {

        val input = text.text
        val builder = AnnotatedString.Builder()

        var i = 0

        while (i < input.length) {
            if (input[i] == '*' || input[i] == '_' || input[i] == '~') {
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
                        '~' -> SpanStyle(textDecoration = TextDecoration.Underline)
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

        val result = builder.toAnnotatedString()
        val finalBuilder = AnnotatedString.Builder(result)

        // URL detection
        val urlRegex = Regex("""https?://[^\s\n]+""")
        urlRegex.findAll(input).forEach { match ->
            // Clean up trailing punctuation often included in simple regex
            val url = match.value.removeSuffix(".").removeSuffix(",").removeSuffix(";").removeSuffix(")")
            val rangeEnd = match.range.first + url.length

            val linkStyle = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )

            finalBuilder.addStyle(
                linkStyle,
                match.range.first,
                rangeEnd
            )
            
            finalBuilder.addLink(
                LinkAnnotation.Url(url, styles = TextLinkStyles(style = linkStyle)),
                match.range.first,
                rangeEnd
            )
        }

        return finalBuilder.toAnnotatedString()
    }
}
