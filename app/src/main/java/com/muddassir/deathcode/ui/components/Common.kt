package com.muddassir.deathcode.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muddassir.deathcode.data.local.db.dao.PathRow
import com.muddassir.deathcode.domain.model.ContentSource

/** `C++ > DSA > Fundamentals > Loops` */
@Composable
fun Breadcrumb(
    path: List<PathRow>,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null,
) {
    if (path.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        path.forEachIndexed { index, row ->
            if (index > 0) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            val isLast = index == path.lastIndex
            Text(
                text = row.title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isLast) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onNavigate != null && !isLast) {
                    Modifier.padding(vertical = 2.dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** A row of keyword chips (keyboard triggers). */
@Composable
fun KeywordChips(
    keywords: List<String>,
    modifier: Modifier = Modifier,
    onKeywordClick: ((String) -> Unit)? = null,
) {
    if (keywords.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keywords.forEach { keyword ->
            AssistChip(
                onClick = { onKeywordClick?.invoke(keyword) },
                label = {
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/** Small coloured origin label: Official / Mine / Community. */
@Composable
fun SourceBadge(source: ContentSource, modifier: Modifier = Modifier) {
    val (label, color) = when (source) {
        ContentSource.OFFICIAL -> "Official" to MaterialTheme.colorScheme.secondary
        ContentSource.PRIVATE -> "Mine" to MaterialTheme.colorScheme.primary
        ContentSource.COMMUNITY -> "Community" to MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            action?.invoke()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
