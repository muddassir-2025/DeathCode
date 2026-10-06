package com.muddassir.deathcode.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.data.local.db.entity.CommunitySubmissionEntity
import com.muddassir.deathcode.ui.browse.ContentEditorDialog
import com.muddassir.deathcode.ui.components.EmptyState
import com.muddassir.deathcode.ui.rememberAppViewModel

/**
 * Community: a curated, moderated programming-content library — deliberately not a social
 * network. No messaging, no followers, no comments.
 *
 * Approved content is browsed from the local database (offline capable); submitting is the
 * only action that needs an account and the network, and the server decides whether content
 * becomes public.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(onOpenNode: (String) -> Unit) {
    val viewModel: CommunityViewModel = rememberAppViewModel { CommunityViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(onClick = { showEditor = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New submission")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Browse") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("My submissions") },
                )
            }

            state.message?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
                }
            }

            if (tab == 0) {
                CommunityBrowser(
                    state = state,
                    onOpenNode = onOpenNode,
                    onSync = viewModel::syncCommunity,
                )
            } else {
                MySubmissions(
                    state = state,
                    onSignIn = { showAuth = true },
                    onSubmit = viewModel::submit,
                    onDelete = viewModel::delete,
                    onSignOut = viewModel::signOut,
                    onSync = viewModel::syncCommunity,
                )
            }
        }
    }

    if (showEditor) {
        ContentEditorDialog(
            title = "Contribute to Community",
            confirmLabel = "Save draft",
            onDismiss = { showEditor = false },
            onSave = { markdown, syntax, notes, language, keywords ->
                viewModel.createDraft(
                    title = markdown.lineSequence().firstOrNull { it.isNotBlank() }?.removePrefix("#")
                        ?.trim() ?: "Untitled submission",
                    markdown = markdown,
                    syntax = syntax,
                    language = language,
                    keywords = keywords,
                )
                showEditor = false
            },
        )
    }

    if (showAuth) {
        AuthDialog(
            onDismiss = { showAuth = false },
            onSignIn = { email, password -> viewModel.signIn(email, password); showAuth = false },
            onSignUp = { email, password -> viewModel.signUp(email, password); showAuth = false },
        )
    }
}

@Composable
private fun CommunityBrowser(
    state: CommunityState,
    onOpenNode: (String) -> Unit,
    onSync: () -> Unit,
) {
    if (state.roots.isEmpty()) {
        EmptyState(
            title = "No community content yet",
            message = "Approved community submissions appear here after synchronization. " +
                "Everything shown is stored locally and readable offline.",
            action = {
                Button(onClick = onSync, enabled = !state.syncing) {
                    Text(if (state.syncing) "Checking..." else "Check for updates")
                }
            },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.roots, key = { it.id }) { node ->
            Card(
                onClick = { onOpenNode(node.id) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(node.title, style = MaterialTheme.typography.titleMedium)
                    val preview = node.markdown.lineSequence().firstOrNull { it.isNotBlank() }
                    if (preview != null) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MySubmissions(
    state: CommunityState,
    onSignIn: () -> Unit,
    onSubmit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.session.isSignedIn) {
                    "Signed in as ${state.session.email}"
                } else {
                    "Not signed in"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.session.isSignedIn) {
                TextButton(onClick = onSignOut) { Text("Sign out") }
            } else {
                TextButton(onClick = onSignIn) { Text("Sign in") }
            }
        }

        HorizontalDivider()

        if (state.submissions.isEmpty()) {
            EmptyState(
                title = "No submissions",
                message = "Draft programming notes, templates or explanations here. They stay " +
                    "on your device until you explicitly submit them.",
            )
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.submissions, key = { it.id }) { submission ->
                SubmissionCard(
                    submission = submission,
                    onSubmit = { onSubmit(submission.id) },
                    onDelete = { onDelete(submission.id) },
                )
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: CommunitySubmissionEntity,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(submission.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Status: ${submission.status}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            submission.reviewMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSubmit) { Text("Submit for review") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

/** Login / sign-up. Only needed to submit to Community. */
@Composable
private fun AuthDialog(
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Community account") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(min = 140.dp),
            ) {
                Text(
                    text = "You only need an account to submit content for review. " +
                        "Everything else works without signing in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { onSignIn(email, password) },
                    enabled = email.isNotBlank() && password.isNotBlank(),
                ) { Text("Sign in") }
                TextButton(
                    onClick = { onSignUp(email, password) },
                    enabled = email.isNotBlank() && password.length >= 6,
                ) { Text("Sign up") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
