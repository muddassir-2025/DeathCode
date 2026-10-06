package com.muddassir.deathcode.data.seed

import com.muddassir.deathcode.data.remote.ContentChecksum
import com.muddassir.deathcode.data.remote.dto.ContentNodeDto
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto

/**
 * The official Death Code content that ships inside the APK.
 *
 * The app is usable immediately after install because this package is applied through the
 * exact same validated pipeline as a downloaded update — no network request is ever needed
 * for the first run.
 */
object BundledOfficialContent {

    const val PACKAGE_VERSION = 1L

    fun packageDto(): ContentPackageDto {
        val nodes = buildList {
            addAll(cpp())
            addAll(python())
            addAll(dsa())
            addAll(databases())
            addAll(systems())
        }
        return ContentPackageDto(
            packageVersion = PACKAGE_VERSION,
            generatedAt = 0L,
            checksum = ContentChecksum.compute(nodes),
            kind = "OFFICIAL",
            // The bundle is the complete official set that ships with the app.
            fullSnapshot = true,
            nodes = nodes,
        )
    }

    // ------------------------------------------------------------------ C++

    private fun cpp(): List<ContentNodeDto> = listOf(
        node(
            id = "cpp",
            title = "C++",
            sortOrder = 0,
            markdown = "C++ is a compiled, statically typed language used for competitive " +
                "programming, systems work and performance critical software.",
        ),
        node(
            id = "cpp-dsa",
            parentId = "cpp",
            title = "DSA",
            sortOrder = 0,
            markdown = "Data structures and algorithms implemented in C++.",
        ),
        node(
            id = "cpp-dsa-fundamentals",
            parentId = "cpp-dsa",
            title = "Fundamentals",
            sortOrder = 0,
        ),
        node(
            id = "cpp-dsa-fundamentals-datatypes",
            parentId = "cpp-dsa-fundamentals",
            title = "Data Types",
            sortOrder = 0,
            markdown = "C++ fundamental and fixed width integer types.",
            syntax = "int          // 4 bytes (usually)\nlong long    // 8 bytes\nunsigned int // 4 bytes, no negatives\ndouble       // 8 bytes\nchar         // 1 byte\nbool         // 1 byte",
            language = "cpp",
            keywords = listOf("int", "long", "double", "datatype"),
        ),
        node(
            id = "cpp-dsa-fundamentals-loops",
            parentId = "cpp-dsa-fundamentals",
            title = "Loops",
            sortOrder = 1,
            markdown = "Loops are used to execute a block of code repeatedly.",
        ),
        node(
            id = "cpp-dsa-fundamentals-loops-for",
            parentId = "cpp-dsa-fundamentals-loops",
            title = "For Loop",
            sortOrder = 0,
            markdown = "The **for** loop is useful when the number of iterations is known " +
                "up front.\n\nIt combines initialization, condition and increment in one line.",
            syntax = "for (int i = 0; i < n; i++) {\n\n}",
            notes = "Use `long long` when the iteration variable may exceed the range of " +
                "`int`. Starting from `1` is common when the problem is 1-indexed.",
            language = "cpp",
            keywords = listOf("for", "forloop", "loop"),
        ),
        node(
            id = "cpp-dsa-fundamentals-loops-while",
            parentId = "cpp-dsa-fundamentals-loops",
            title = "While Loop",
            sortOrder = 1,
            markdown = "A **while** loop repeats as long as its condition stays true.",
            syntax = "while (condition) {\n\n}",
            language = "cpp",
            keywords = listOf("while", "whileloop", "loop"),
        ),
        node(
            id = "cpp-dsa-fundamentals-loops-dowhile",
            parentId = "cpp-dsa-fundamentals-loops",
            title = "Do While Loop",
            sortOrder = 2,
            markdown = "A **do while** loop always executes its body at least once.",
            syntax = "do {\n\n} while (condition);",
            language = "cpp",
            keywords = listOf("dowhile", "loop"),
        ),
        node(
            id = "cpp-dsa-fundamentals-functions",
            parentId = "cpp-dsa-fundamentals",
            title = "Functions",
            sortOrder = 2,
            markdown = "Functions group reusable logic. Prefer passing large containers by " +
                "reference to avoid copies.",
            syntax = "int add(int a, int b) {\n    return a + b;\n}",
            language = "cpp",
            keywords = listOf("function", "return"),
        ),
        node(
            id = "cpp-builtins",
            parentId = "cpp-dsa",
            title = "C++ Built-in Functions",
            sortOrder = 1,
            markdown = "Standard library helpers that are available offline in the reference.",
        ),
        node(
            id = "cpp-builtins-vector",
            parentId = "cpp-builtins",
            title = "Vector",
            sortOrder = 0,
            markdown = "`std::vector` is a dynamic array and the default container in modern C++.",
        ),
        node(
            id = "cpp-builtins-vector-pushback",
            parentId = "cpp-builtins-vector",
            title = "push_back()",
            sortOrder = 0,
            markdown = "Appends an element to the end of the vector.",
            syntax = "v.push_back(42);",
            language = "cpp",
            keywords = listOf("pushback", "vector"),
        ),
        node(
            id = "cpp-builtins-vector-popback",
            parentId = "cpp-builtins-vector",
            title = "pop_back()",
            sortOrder = 1,
            markdown = "Removes the last element. The behaviour is undefined on an empty vector.",
            syntax = "v.pop_back();",
            language = "cpp",
            keywords = listOf("popback", "vector"),
        ),
        node(
            id = "cpp-builtins-vector-size",
            parentId = "cpp-builtins-vector",
            title = "size()",
            sortOrder = 2,
            markdown = "Returns the number of elements currently stored.",
            syntax = "v.size();",
            language = "cpp",
            keywords = listOf("size", "vector"),
        ),
        node(
            id = "cpp-builtins-algorithms",
            parentId = "cpp-builtins",
            title = "Algorithms",
            sortOrder = 1,
            markdown = "Functions from `<algorithm>` that operate on ranges.",
        ),
        node(
            id = "cpp-builtins-algorithms-sort",
            parentId = "cpp-builtins-algorithms",
            title = "sort()",
            sortOrder = 0,
            markdown = "The sort function orders elements in a range.",
            syntax = "sort(v.begin(), v.end());",
            notes = "Complexity is `O(n log n)`. Pass a comparator for custom ordering.",
            language = "cpp",
            keywords = listOf("sort", "sorting"),
        ),
        node(
            id = "cpp-builtins-algorithms-reverse",
            parentId = "cpp-builtins-algorithms",
            title = "reverse()",
            sortOrder = 1,
            markdown = "Reverses the selected range in place.",
            syntax = "reverse(v.begin(), v.end());",
            language = "cpp",
            keywords = listOf("reverse"),
        ),
        node(
            id = "cpp-builtins-algorithms-lowerbound",
            parentId = "cpp-builtins-algorithms",
            title = "lower_bound()",
            sortOrder = 2,
            markdown = "Finds the first position where the value can be inserted without " +
                "breaking the sort order. The range must be sorted.",
            syntax = "auto it = lower_bound(v.begin(), v.end(), x);\nint index = it - v.begin();",
            language = "cpp",
            keywords = listOf("lowerbound", "binarysearch"),
        ),
        node(
            id = "cpp-builtins-algorithms-upperbound",
            parentId = "cpp-builtins-algorithms",
            title = "upper_bound()",
            sortOrder = 3,
            markdown = "Finds the first element strictly greater than the given value.",
            syntax = "auto it = upper_bound(v.begin(), v.end(), x);",
            language = "cpp",
            keywords = listOf("upperbound", "binarysearch"),
        ),
        node(
            id = "cpp-graphs",
            parentId = "cpp-dsa",
            title = "Graphs",
            sortOrder = 2,
            markdown = "Graph traversal and shortest path templates.",
        ),
        node(
            id = "cpp-graphs-bfs",
            parentId = "cpp-graphs",
            title = "BFS",
            sortOrder = 0,
            markdown = "Breadth first search visits neighbours level by level and finds the " +
                "shortest path in an unweighted graph.",
            syntax = "queue<int> q;\nvector<int> dist(n, -1);\ndist[src] = 0;\nq.push(src);\nwhile (!q.empty()) {\n    int u = q.front(); q.pop();\n    for (int v : adj[u]) {\n        if (dist[v] == -1) {\n            dist[v] = dist[u] + 1;\n            q.push(v);\n        }\n    }\n}",
            language = "cpp",
            keywords = listOf("bfs", "breadth", "graph"),
        ),
        node(
            id = "cpp-graphs-dfs",
            parentId = "cpp-graphs",
            title = "DFS",
            sortOrder = 1,
            markdown = "Depth first search explores as far as possible before backtracking.",
            syntax = "void dfs(int u) {\n    visited[u] = true;\n    for (int v : adj[u]) {\n        if (!visited[v]) dfs(v);\n    }\n}",
            language = "cpp",
            keywords = listOf("dfs", "depth", "graph"),
        ),
    )

    // --------------------------------------------------------------- Python

    private fun python(): List<ContentNodeDto> = listOf(
        node(
            id = "python",
            title = "Python",
            sortOrder = 1,
            markdown = "Python is a readable, dynamically typed language widely used for " +
                "automation, data work and scripting.",
        ),
        node(
            id = "python-basics",
            parentId = "python",
            title = "Basics",
            sortOrder = 0,
        ),
        node(
            id = "python-basics-variables",
            parentId = "python-basics",
            title = "Variables",
            sortOrder = 0,
            markdown = "Python variables are names bound to objects; no type declaration is needed.",
            syntax = "count = 0\nname = \"deathcode\"\nratio = 0.5",
            language = "python",
            keywords = listOf("variable", "assignment"),
        ),
        node(
            id = "python-basics-loops",
            parentId = "python-basics",
            title = "Loops",
            sortOrder = 1,
        ),
        node(
            id = "python-basics-loops-for",
            parentId = "python-basics-loops",
            title = "For Loop",
            sortOrder = 0,
            markdown = "Iterate directly over any iterable with `for`.",
            syntax = "for i in range(n):\n    print(i)",
            notes = "`range(start, stop, step)` is half open: `stop` is excluded.",
            language = "python",
            keywords = listOf("for", "forloop", "loop"),
        ),
        node(
            id = "python-basics-loops-while",
            parentId = "python-basics-loops",
            title = "While Loop",
            sortOrder = 1,
            markdown = "Repeat while a condition holds.",
            syntax = "while condition:\n    ...",
            language = "python",
            keywords = listOf("while", "loop"),
        ),
        node(
            id = "python-data-structures",
            parentId = "python",
            title = "Data Structures",
            sortOrder = 1,
        ),
        node(
            id = "python-ds-list",
            parentId = "python-data-structures",
            title = "List",
            sortOrder = 0,
            markdown = "A mutable, ordered sequence.",
            syntax = "nums = [1, 2, 3]\nnums.append(4)\nnums.sort()",
            language = "python",
            keywords = listOf("list", "array"),
        ),
        node(
            id = "python-ds-dict",
            parentId = "python-data-structures",
            title = "Dictionary",
            sortOrder = 1,
            markdown = "Key value mapping with average `O(1)` lookup.",
            syntax = "freq = {}\nfreq[key] = freq.get(key, 0) + 1",
            language = "python",
            keywords = listOf("dict", "dictionary", "hashmap"),
        ),
        node(
            id = "python-ds-set",
            parentId = "python-data-structures",
            title = "Set",
            sortOrder = 2,
            markdown = "An unordered collection of unique elements.",
            syntax = "seen = set()\nseen.add(x)\nif x in seen:\n    ...",
            language = "python",
            keywords = listOf("set", "unique"),
        ),
    )

    // ------------------------------------------------------------------ DSA

    private fun dsa(): List<ContentNodeDto> = listOf(
        node(
            id = "dsa",
            title = "DSA",
            sortOrder = 2,
            markdown = "Language independent data structures and algorithms.",
        ),
        node(
            id = "dsa-arrays",
            parentId = "dsa",
            title = "Arrays",
            sortOrder = 0,
        ),
        node(
            id = "dsa-arrays-twopointer",
            parentId = "dsa-arrays",
            title = "Two Pointer",
            sortOrder = 0,
            markdown = "Move two indices towards each other to solve many array problems in " +
                "linear time.",
            syntax = "int l = 0, r = n - 1;\nwhile (l < r) {\n    // use a[l] and a[r]\n    if (shouldMoveLeft) l++; else r--;\n}",
            language = "cpp",
            keywords = listOf("twopointer", "twopointers"),
        ),
        node(
            id = "dsa-arrays-slidingwindow",
            parentId = "dsa-arrays",
            title = "Sliding Window",
            sortOrder = 1,
            markdown = "Maintain a window `[l, r]` and update an aggregate as the window moves.",
            syntax = "int l = 0;\nfor (int r = 0; r < n; r++) {\n    // add a[r] to the window\n    while (invalid()) {\n        // remove a[l] from the window\n        l++;\n    }\n    // window [l, r] is valid\n}",
            language = "cpp",
            keywords = listOf("slidingwindow", "window"),
        ),
        node(
            id = "dsa-searching",
            parentId = "dsa",
            title = "Searching",
            sortOrder = 1,
        ),
        node(
            id = "dsa-searching-binarysearch",
            parentId = "dsa-searching",
            title = "Binary Search",
            sortOrder = 0,
            markdown = "Repeatedly halve a sorted range. Complexity `O(log n)`.",
            syntax = "int lo = 0, hi = n - 1;\nwhile (lo <= hi) {\n    int mid = lo + (hi - lo) / 2;\n    if (a[mid] == target) return mid;\n    if (a[mid] < target) lo = mid + 1; else hi = mid - 1;\n}\nreturn -1;",
            notes = "Use `lo + (hi - lo) / 2` to avoid overflow.",
            language = "cpp",
            keywords = listOf("binarysearch", "bs", "search"),
        ),
        node(
            id = "dsa-sorting",
            parentId = "dsa",
            title = "Sorting",
            sortOrder = 2,
        ),
        node(
            id = "dsa-sorting-mergesort",
            parentId = "dsa-sorting",
            title = "Merge Sort",
            sortOrder = 0,
            markdown = "Divide and conquer sort with guaranteed `O(n log n)` time and stable " +
                "ordering.",
            language = "cpp",
            keywords = listOf("mergesort", "sorting"),
        ),
        node(
            id = "dsa-sorting-quicksort",
            parentId = "dsa-sorting",
            title = "Quick Sort",
            sortOrder = 1,
            markdown = "Partition around a pivot, then sort both halves. Average `O(n log n)`, " +
                "worst case `O(n^2)`.",
            language = "cpp",
            keywords = listOf("quicksort", "sorting"),
        ),
        node(
            id = "dsa-number-systems",
            parentId = "dsa",
            title = "Number Systems",
            sortOrder = 3,
        ),
        node(
            id = "dsa-number-systems-decimal-to-binary",
            parentId = "dsa-number-systems",
            title = "Decimal to Binary",
            sortOrder = 0,
            markdown = "To convert a decimal number into a binary string, repeatedly divide by " +
                "two and collect the remainders, then reverse them.\n\nAn alternative is to " +
                "inspect each bit with shifts and masks.",
            syntax = "string toBinary(int n) {\n    if (n == 0) return \"0\";\n    string bits;\n    while (n > 0) {\n        bits += char('0' + (n & 1));\n        n >>= 1;\n    }\n    reverse(bits.begin(), bits.end());\n    return bits;\n}",
            notes = "`n & 1` reads the least significant bit; `n >>= 1` shifts right by one.",
            language = "cpp",
            keywords = listOf("binary", "decimal", "convert", "basetwo"),
        ),
    )

    // ------------------------------------------------------------ Databases

    private fun databases(): List<ContentNodeDto> = listOf(
        node(
            id = "databases",
            title = "Databases",
            sortOrder = 3,
            markdown = "Relational and non relational storage fundamentals.",
        ),
        node(
            id = "databases-sql",
            parentId = "databases",
            title = "SQL",
            sortOrder = 0,
        ),
        node(
            id = "databases-sql-select",
            parentId = "databases-sql",
            title = "SELECT",
            sortOrder = 0,
            markdown = "Read rows from a table, optionally filtered and ordered.",
            syntax = "SELECT id, name\nFROM users\nWHERE active = true\nORDER BY name\nLIMIT 20;",
            language = "sql",
            keywords = listOf("select", "query"),
        ),
        node(
            id = "databases-sql-join",
            parentId = "databases-sql",
            title = "JOIN",
            sortOrder = 1,
            markdown = "Combine rows from two tables using a related column.",
            syntax = "SELECT u.name, o.total\nFROM users u\nJOIN orders o ON o.user_id = u.id;",
            language = "sql",
            keywords = listOf("join", "innerjoin"),
        ),
        node(
            id = "databases-sql-index",
            parentId = "databases-sql",
            title = "INDEX",
            sortOrder = 2,
            markdown = "Indexes speed up reads at the cost of write time and storage.",
            syntax = "CREATE INDEX idx_users_email ON users (email);",
            language = "sql",
            keywords = listOf("index", "performance"),
        ),
        node(
            id = "databases-postgres",
            parentId = "databases",
            title = "PostgreSQL",
            sortOrder = 1,
            markdown = "Death Code's server side store. The Android app never connects to it " +
                "directly; it talks to the Death Code API over HTTPS.",
            language = "sql",
            keywords = listOf("postgres", "postgresql", "neon"),
        ),
    )

    // --------------------------------------------------------------- Systems

    private fun systems(): List<ContentNodeDto> = listOf(
        node(
            id = "operating-systems",
            title = "Operating Systems",
            sortOrder = 4,
        ),
        node(
            id = "os-processes",
            parentId = "operating-systems",
            title = "Processes",
            sortOrder = 0,
            markdown = "A process is an instance of a running program with its own address space.",
            keywords = listOf("process", "thread"),
        ),
        node(
            id = "os-scheduling",
            parentId = "operating-systems",
            title = "Scheduling",
            sortOrder = 1,
            markdown = "The scheduler decides which ready process runs on the CPU next.",
            keywords = listOf("scheduling", "cpu"),
        ),
        node(
            id = "computer-networks",
            title = "Computer Networks",
            sortOrder = 5,
        ),
        node(
            id = "networks-http",
            parentId = "computer-networks",
            title = "HTTP",
            sortOrder = 0,
            markdown = "Request/response application protocol. Death Code synchronizes content " +
                "over HTTPS JSON endpoints.",
            keywords = listOf("http", "https", "rest"),
        ),
        node(
            id = "networks-tcp",
            parentId = "computer-networks",
            title = "TCP",
            sortOrder = 1,
            markdown = "Reliable, ordered, connection oriented transport protocol.",
            keywords = listOf("tcp", "transport"),
        ),
    )

    private fun node(
        id: String,
        title: String,
        parentId: String? = null,
        markdown: String = "",
        syntax: String? = null,
        notes: String? = null,
        language: String? = null,
        keywords: List<String> = emptyList(),
        sortOrder: Int = 0,
    ) = ContentNodeDto(
        id = id,
        parentId = parentId,
        title = title,
        slug = id,
        markdown = markdown,
        syntax = syntax,
        notes = notes,
        language = language,
        keywords = keywords,
        sortOrder = sortOrder,
    )
}
