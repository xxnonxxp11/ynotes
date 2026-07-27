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
    private var lastText: String? = null
    private var lastResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val currentText = text.text
        if (currentText == lastText && lastResult != null) {
            return lastResult!!
        }
        // We use our parse function directly to format the typing text
        // Because we don't change the length of the string, OffsetMapping.Identity works perfectly!
        val formattedText = parseMarkdownSync(currentText, baseColor)
        val result = TransformedText(formattedText, OffsetMapping.Identity)
        lastText = currentText
        lastResult = result
        return result
    }
}

private val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
private val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)|_(.*?)_")
private val strikeRegex = Regex("~~(.*?)~~")
private val headerRegex = Regex("(?m)^(#{1,3}) (.*)$")
private val codeRegex = Regex("`(.*?)`")
private val quoteRegex = Regex("(?m)^> (.*)$")

private val parseCache = android.util.LruCache<String, AnnotatedString>(64)

// A synchronous version of parseMarkdown for the VisualTransformation that doesn't need @Composable
fun parseMarkdownSync(text: String, baseColor: Color): AnnotatedString {
    val cacheKey = "${text.hashCode()}_${baseColor.value}"
    parseCache.get(cacheKey)?.let { return it }

    val result = buildAnnotatedString {
        append(text)
        
        // Bold: **text**
        boldRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 2)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last - 1, match.range.last + 1)
        }
        
        // Italic: *text* or _text_
        italicRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last, match.range.last + 1)
        }
        
        // Strikethrough: ~~text~~
        strikeRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + 2)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.last - 1, match.range.last + 1)
        }
        
        // Headers: # Header (1, 2, or 3)
        headerRegex.findAll(text).forEach { match ->
            val level = match.groupValues[1].length
            val (fontSize, prefixLen) = when (level) {
                1 -> 24.sp to 2
                2 -> 20.sp to 3
                else -> 18.sp to 4
            }
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = baseColor.copy(alpha = 0.3f)), match.range.first, match.range.first + prefixLen)
        }
        
        // Code inline: `code`
        val codeBgColor = baseColor.copy(alpha = 0.1f)
        codeRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(background = codeBgColor, color = baseColor.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
        }
        
        // Quotes: > Quote
        val quoteColor = baseColor.copy(alpha = 0.6f)
        quoteRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
    }

    parseCache.put(cacheKey, result)
    return result
}
