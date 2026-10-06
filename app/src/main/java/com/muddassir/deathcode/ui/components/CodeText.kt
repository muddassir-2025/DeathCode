package com.muddassir.deathcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muddassir.deathcode.markdown.InlineSpan
import com.muddassir.deathcode.markdown.MarkdownBlock
import com.muddassir.deathcode.markdown.MarkdownBlockParser
import com.muddassir.deathcode.syntax.Languages
import com.muddassir.deathcode.syntax.SyntaxHighlighter
import com.muddassir.deathcode.syntax.TokenKind
import com.muddassir.deathcode.ui.theme.CodeTextStyle
import com.muddassir.deathcode.ui.theme.LocalCodeColors

/** A syntax highlighted, horizontally scrollable code block with a copy action. */
@Composable
fun CodeBlock(
    code: String,
    language: String?,
    modifier: Modifier = Modifier,
    showLanguageLabel: Boolean = true,
) {
    val colors = LocalCodeColors.current
    val clipboard = LocalClipboardManager.current

    val annotated = remember(code, language, colors) {
        buildAnnotatedString {
            SyntaxHighlighter.tokenize(code, language).forEach { token ->
                withStyle(
                    SpanStyle(
                        color = when (token.kind) {
                            TokenKind.PLAIN -> colors.plain
                            TokenKind.KEYWORD -> colors.keyword
                            TokenKind.TYPE -> colors.type
                            TokenKind.STRING -> colors.string
                            TokenKind.COMMENT -> colors.comment
                            TokenKind.NUMBER -> colors.number
                            TokenKind.FUNCTION -> colors.function
                            TokenKind.ANNOTATION -> colors.annotation
                        },
                        fontWeight = if (token.kind == TokenKind.KEYWORD) FontWeight.Medium else null,
                        fontStyle = if (token.kind == TokenKind.COMMENT) FontStyle.Italic else null,
                    ),
                ) {
                    append(token.text)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (showLanguageLabel) Languages.displayName(language) else "",
                style = MaterialTheme.typography.labelSmall,
                color = colors.comment,
            )
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.comment,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            Text(
                text = annotated,
                style = CodeTextStyle,
            )
        }
    }
}

/** Renders markdown documentation into the Death Code reader UI. */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { MarkdownBlockParser.parse(markdown) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block -> MarkdownBlockView(block) }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
        )

        is MarkdownBlock.Paragraph -> Text(
            text = inlineText(block.text),
            style = MaterialTheme.typography.bodyLarge,
        )

        is MarkdownBlock.CodeBlock -> CodeBlock(block.code, block.language)

        is MarkdownBlock.ListBlock -> Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            block.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(start = (item.depth * 16).dp)) {
                    Text(
                        text = if (block.ordered) "${block.start + index}. " else "• ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = inlineText(item.text),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        is MarkdownBlock.Quote -> Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 2.dp),
            ) {}
            Text(
                text = inlineText(block.text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        is MarkdownBlock.Table -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Row {
                block.headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            block.rows.forEach { row ->
                Row {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }

        MarkdownBlock.Divider -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .padding(top = 1.dp),
        ) {}
    }
}

/** Builds an [AnnotatedString] from inline markdown spans. */
@Composable
fun inlineText(text: String): AnnotatedString {
    val colors = LocalCodeColors.current
    val spans = remember(text) { MarkdownBlockParser.parseInline(text) }

    return remember(text, colors) {
        buildAnnotatedString {
            spans.forEach { span ->
                when (span) {
                    is InlineSpan.Plain -> append(span.text)
                    is InlineSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(span.text)
                    }
                    is InlineSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(span.text)
                    }
                    is InlineSpan.Code -> withStyle(
                        SpanStyle(
                            fontFamily = com.muddassir.deathcode.ui.theme.CodeFontFamily,
                            background = colors.background,
                            color = colors.function,
                            fontSize = 14.sp,
                        ),
                    ) {
                        append(" ${span.text} ")
                    }
                    is InlineSpan.Link -> withStyle(
                        SpanStyle(
                            color = colors.type,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(span.text)
                    }
                }
            }
        }
    }
}

/** Convenience for showing plain code inline (e.g. in cards). */
@Composable
fun MonoText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text = text,
        style = CodeTextStyle,
        color = color,
        modifier = modifier,
    )
}
