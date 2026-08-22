package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Lightweight markdown rendering for AI replies so they read like Gemini/Claude:
 *   - **bold** / __bold__
 *   - *italic* / _italic_
 *   - `inline code`
 *   - headings (#, ##, ###)
 *   - bullet lists (-, *, •) and numbered lists
 *   - block quotes (>)
 *   - code fences (```)
 */
@Composable
fun MarkdownLiteText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    val annotated = remember(text, resolvedColor, style.fontSize, style.fontWeight) {
        buildMarkdown(text, resolvedColor, style)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style
    )
}

private fun buildMarkdown(text: String, color: Color, style: TextStyle): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        var inCode = false
        for ((i, rawLine) in lines.withIndex()) {
            val line = rawLine
            if (line.trimStart().startsWith("```")) {
                inCode = !inCode
                continue
            }
            if (inCode) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000), color = color)) {
                    append(line)
                }
                if (i < lines.size - 1) append('\n')
                continue
            }
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (style.fontSize.value + 3).sp, color = color)) { append(trimmed.removePrefix("### ")) }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (style.fontSize.value + 4).sp, color = color)) { append(trimmed.removePrefix("## ")) }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (style.fontSize.value + 5).sp, color = color)) { append(trimmed.removePrefix("# ")) }
                }
                trimmed.startsWith("> ") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color.copy(alpha = 0.8f))) { append(trimmed.removePrefix("> ")) }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    append("•  ")
                    appendInline(this, trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• "), color)
                }
                else -> {
                    val num = Regex("""^(\d+)[.)]\s+(.*)$""").find(trimmed)
                    if (num != null) {
                        append("${num.groupValues[1]}.  ")
                        appendInline(this, num.groupValues[2], color)
                    } else {
                        appendInline(this, line, color)
                    }
                }
            }
            if (i < lines.size - 1) append('\n')
        }
    }
}

/** A tiny inline parser for bold / italic / inline-code. */
private fun appendInline(ab: AnnotatedString.Builder, s: String, color: Color) {
    val regex = Regex("""(\*\*|__)(.+?)\1|(\*|_)(.+?)\3|(`)(.+?)`""")
    var last = 0
    for (m in regex.findAll(s)) {
        if (m.range.first > last) ab.append(s.substring(last, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> ab.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(g[2]) }
            g[3].isNotEmpty() -> ab.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) { append(g[4]) }
            g[5].isNotEmpty() -> ab.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000), color = color)) { append(g[6]) }
        }
        last = m.range.last + 1
    }
    if (last < s.length) ab.append(s.substring(last))
}
