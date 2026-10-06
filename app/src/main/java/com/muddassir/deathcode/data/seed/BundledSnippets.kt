package com.muddassir.deathcode.data.seed

import com.muddassir.deathcode.domain.model.ContentSource

/** A snippet that ships with the app. */
data class SeedSnippet(
    val title: String,
    val language: String,
    val body: String,
    val keywords: List<String>,
    val source: ContentSource = ContentSource.OFFICIAL,
)

/**
 * Starter templates so the Death Code Keyboard is useful the moment it is enabled.
 *
 * The same keyword (`bfs`) intentionally exists for several languages — the keyboard picks
 * the template that matches the active keyboard language.
 */
object BundledSnippets {

    fun all(): List<SeedSnippet> = listOf(
        SeedSnippet(
            title = "For Loop",
            language = "cpp",
            body = "for (int i = 0; i < n; i++) {\n    \${cursor}\n}",
            keywords = listOf("for", "forloop", "loop"),
        ),
        SeedSnippet(
            title = "For Loop",
            language = "python",
            body = "for i in range(n):\n    \${cursor}",
            keywords = listOf("for", "forloop", "loop"),
        ),
        SeedSnippet(
            title = "For Loop",
            language = "java",
            body = "for (int i = 0; i < n; i++) {\n    \${cursor}\n}",
            keywords = listOf("for", "forloop", "loop"),
        ),
        SeedSnippet(
            title = "While Loop",
            language = "cpp",
            body = "while (\${condition}) {\n    \${cursor}\n}",
            keywords = listOf("while", "whileloop"),
        ),
        SeedSnippet(
            title = "BFS",
            language = "cpp",
            body = "queue<int> q;\nvector<int> dist(n, -1);\ndist[\${src}] = 0;\nq.push(\${src});\nwhile (!q.empty()) {\n    int u = q.front(); q.pop();\n    for (int v : adj[u]) {\n        if (dist[v] == -1) {\n            dist[v] = dist[u] + 1;\n            q.push(v);\n        }\n    }\n}\n\${cursor}",
            keywords = listOf("bfs", "breadth", "graph"),
        ),
        SeedSnippet(
            title = "BFS",
            language = "python",
            body = "from collections import deque\n\nq = deque([\${src}])\ndist = {src: 0}\nwhile q:\n    u = q.popleft()\n    for v in adj[u]:\n        if v not in dist:\n            dist[v] = dist[u] + 1\n            q.append(v)\n\${cursor}",
            keywords = listOf("bfs", "breadth", "graph"),
        ),
        SeedSnippet(
            title = "DFS",
            language = "cpp",
            body = "void dfs(int u) {\n    visited[u] = true;\n    for (int v : adj[u]) {\n        if (!visited[v]) dfs(v);\n    }\n}",
            keywords = listOf("dfs", "depth", "graph"),
        ),
        SeedSnippet(
            title = "Binary Search",
            language = "cpp",
            body = "int lo = 0, hi = n - 1;\nwhile (lo <= hi) {\n    int mid = lo + (hi - lo) / 2;\n    if (a[mid] == \${target}) {\n        \${cursor}\n    } else if (a[mid] < \${target}) {\n        lo = mid + 1;\n    } else {\n        hi = mid - 1;\n    }\n}",
            keywords = listOf("binarysearch", "bs", "search"),
        ),
        SeedSnippet(
            title = "Fast IO",
            language = "cpp",
            body = "ios::sync_with_stdio(false);\ncin.tie(nullptr);",
            keywords = listOf("fastio", "io"),
        ),
        SeedSnippet(
            title = "Vector Initialization",
            language = "cpp",
            body = "vector<int> v(n, \${0});",
            keywords = listOf("vector", "inita"),
        ),
        SeedSnippet(
            title = "Sort Vector",
            language = "cpp",
            body = "sort(v.begin(), v.end());",
            keywords = listOf("sort", "sorting"),
        ),
        SeedSnippet(
            title = "Map Iteration",
            language = "cpp",
            body = "for (const auto& [key, value] : \${map}) {\n    \${cursor}\n}",
            keywords = listOf("foreach", "iterate", "map"),
        ),

        // A private starter template so the personal-keyword flow works out of the box:
        // typing `triangle` in the keyboard offers the user's own template.
        SeedSnippet(
            title = "Pattern Matching",
            language = "cpp",
            body = "for (int i = 1; i <= n; i++) {\n    for (int j = 1; j <= i; j++) {\n        cout << \"* \";\n    }\n    cout << \"\\n\";\n}\n\${cursor}",
            keywords = listOf("triangle", "pattern", "pyramid"),
            source = ContentSource.PRIVATE,
        ),
    )
}
