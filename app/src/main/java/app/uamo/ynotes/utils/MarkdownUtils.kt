package app.uamo.ynotes.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun parseMarkdown(text: String, baseColor: Color = MaterialTheme.colorScheme.onBackground): AnnotatedString {
    return parseMarkdownSync(text, baseColor)
}

class MarkdownVisualTransformation(private val baseColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // We use our parse function directly to format the typing text
        // Because we don't change the length of the string, OffsetMapping.Identity works perfectly!
        val formattedText = parseMarkdownSync(text.text, baseColor)
        return TransformedText(formattedText, OffsetMapping.Identity)
    }
}

// A synchronous version of parseMarkdown for the VisualTransformation that doesn't need @Composable
fun parseMarkdownSync(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        // Bold: **text**
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 2)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last - 1, match.range.last + 1)
        }
        
        // Italic: *text* or _text_
        val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|_(.*?)_")
        italicRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last, match.range.last + 1)
        }
        
        // Strikethrough: ~~text~~
        val strikeRegex = Regex("~~(.*?)~~")
        strikeRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 2)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last - 1, match.range.last + 1)
        }
        
        // Headers: # Header
        val header1Regex = Regex("(?m)^# (.*)$")
        header1Regex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 2)
        }
        
        val header2Regex = Regex("(?m)^## (.*)$")
        header2Regex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 3)
        }

        val header3Regex = Regex("(?m)^### (.*)$")
        header3Regex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 4)
        }
        
        // Code inline: `code`
        val codeRegex = Regex("`(.*?)`")
        val codeBgColor = baseColor.copy(alpha = 0.1f)
        codeRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(background = codeBgColor, color = baseColor.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
        }
        
        // Quotes: > Quote
        val quoteRegex = Regex("(?m)^> (.*)$")
        val quoteColor = baseColor.copy(alpha = 0.6f)
        quoteRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
    }
}
