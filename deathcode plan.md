# Death Code — Complete App Development Prompt

Build a production-quality Android application called **Death Code** using **Kotlin + Jetpack Compose**.

Death Code is an **offline-first programming knowledge system combined with a dedicated programming keyboard**. It allows users to browse structured programming content, search that content instantly from local storage, maintain private notes and reusable code templates, and use those templates through a custom coding keyboard.

It also includes a moderated **Community** system where authenticated users can optionally submit their own programming content for administrator review.

The most important architectural requirement is:

> **Normal app usage, browsing, searching, notes, snippets, and keyboard suggestions must work from local storage and must not require the backend.**

The backend is primarily a **content synchronization/distribution service and Community service**.

---

# 1. Product Overview

Death Code has four major parts:

1. **Death Code** — official programming knowledge maintained by the Super Admin
2. **My Notes** — completely private user-created programming knowledge and templates
3. **Community** — moderated user-submitted public programming content
4. **Death Code Keyboard** — a custom Android keyboard designed for programmers

The core user loop is:

```text
Learn
  ↓
Save / personalize
  ↓
Attach a reusable template + keyboard keywords
  ↓
Use it from the programming keyboard
  ↓
Search it instantly later
```

The product should feel like a combination of:

```text
Programming documentation
+
Personal developer notebook
+
Code/snippet manager
+
Programming keyboard
```

Do not make it feel like a social media application.

---

# 2. Technology Stack

Use:

- Kotlin
- Jetpack Compose
- Android
- Room for local persistent storage
- SQLite / FTS5 where useful for local search
- DataStore for lightweight preferences
- `InputMethodService` for the custom keyboard
- Neon PostgreSQL for backend persistence
- Render for backend/API hosting
- A secure REST API or equivalent lightweight backend API
- Authentication only for features that genuinely require a server-side identity, primarily Community submissions

Do not allow the Android application to connect directly to PostgreSQL.

Architecture:

```text
Android App
    |
    | HTTPS API
    v
Backend API on Render
    |
    v
Neon PostgreSQL
```

The Android app's runtime data source should be Room.

---

# 3. Authentication

There must be **no mandatory login or sign-up** for normal app usage.

Anonymous users can:

- browse official Death Code content
- search
- use Reading Mode
- create private categories/subcategories
- create private cards
- write private notes
- create personal code templates
- add keyboard keywords
- use the Death Code Keyboard
- configure keyboard settings
- use all normal offline functionality

Authentication is required only when the user wants to use Community functionality that requires an account, such as submitting content for public review.

Desired model:

```text
Anonymous User
    |
    ├── Death Code
    ├── Search
    ├── My Notes
    ├── Personal snippets
    └── Keyboard

Authenticated User
    |
    └── Everything above
         +
         Community submission
```

Do not force login merely to install or use the app.

---

# 4. Fundamental Content Model

The application must use a **recursive tree-based content system with unlimited nesting**.

This is critical.

There is no special limitation such as:

> "Markdown can only create subcategories inside C++."

Instead:

> **Every category/subcategory/content container can independently use either Manual Creation or Markdown Creation, at any depth.**

For example:

```text
C++
└── DSA
    ├── Fundamentals
    │   ├── Data Types
    │   ├── Loops
    │   │   ├── For Loop
    │   │   ├── While Loop
    │   │   └── Do While Loop
    │   └── Functions
    │
    ├── C++ Built-in Functions
    │   ├── sort()
    │   ├── reverse()
    │   ├── lower_bound()
    │   └── upper_bound()
    │
    └── Graphs
        ├── BFS
        └── DFS
```

At **every node**, the authorized creator must be able to:

- manually create child categories/subcategories
- manually create content cards
- import Markdown to generate child content
- edit the Markdown/content
- add notes
- add syntax
- add keyboard keywords
- continue nesting further

The system must not hardcode any hierarchy depth.

---

# 5. Official Content vs Private Content

There are two separate ownership worlds.

## Official Death Code

Created and controlled by the Super Admin.

Users can read it and can add private notes/personalizations associated with it, but they cannot directly modify the official source.

## My Notes

Created and controlled by the user.

The user's tree is completely private and stored locally.

The user can create any structure they want.

Example:

```text
My Notes
├── Interview Prep
│   ├── Arrays
│   ├── Graphs
│   └── Dynamic Programming
│
└── Personal Templates
    ├── BFS
    ├── DFS
    └── Pattern Matching
```

---

# 6. Recursive Manual Creation

Manual creation must be available at **any authorized level**.

Suppose the user is here:

```text
My Notes
└── Interview Prep
```

They can manually add:

```text
Arrays
Graphs
Strings
```

Then enter:

```text
My Notes
└── Interview Prep
    └── Graphs
```

and manually add:

```text
BFS
DFS
Dijkstra
```

Then enter:

```text
My Notes
└── Interview Prep
    └── Graphs
        └── BFS
```

and create or edit the actual content.

The same recursive behavior applies to official content from the admin side.

There should be no restriction on where Manual Creation is available, provided the current user has permission.

---

# 7. Recursive Markdown Creation

Markdown import must also work at **any authorized node**.

Markdown is interpreted **relative to the current location**.

For example, suppose the admin is currently inside:

```text
C++ → DSA
```

and imports:

```markdown
## Fundamentals

### Data Types

### Loops

#### For Loop

#### While Loop

## C++ Built-in Functions

### sort()

### reverse()

### lower_bound()

## Graphs

### BFS

### DFS
```

Those nodes must be created **under C++ → DSA**.

Then suppose the admin navigates into:

```text
C++ → DSA → C++ Built-in Functions
```

and imports another Markdown document:

```markdown
### sort()

The sort function orders elements in a range.

### reverse()

Reverses the selected range.

### lower_bound()

Finds the first position where the value can be inserted.
```

That Markdown must be interpreted relative to:

```text
C++ → DSA → C++ Built-in Functions
```

The resulting hierarchy should be:

```text
C++
└── DSA
    └── C++ Built-in Functions
        ├── sort()
        ├── reverse()
        └── lower_bound()
```

The same operation must work at any depth.

Therefore:

```text
Any Content Node
    |
    ├── Manual Create
    |
    └── Markdown Import
```

This is a core requirement.

---

# 8. Markdown Heading Semantics

Use Markdown heading levels to define relative nesting.

For a Markdown document imported into the currently selected node:

```text
#     = first-level children/content in the imported document
##    = second-level children
###   = third-level children
####  = fourth-level children
...
```

The exact heading offset can be implemented consistently, but the key requirement is:

> **Heading levels define hierarchy relative to the node where the Markdown is imported.**

Do not assume `##` globally means "subcategory of C++."

It only means the corresponding relative depth within the current import.

The parser must prevent malformed hierarchy where necessary and provide a preview before saving.

---

# 9. Markdown as Content

Markdown should not be used only for hierarchy generation.

It is also the actual content format.

A Markdown document can contain:

- headings
- paragraphs
- bold
- italic
- inline code
- fenced code blocks
- unordered lists
- ordered lists
- blockquotes
- tables where practical
- links
- language-specific code blocks

Example:

```markdown
# For Loop

The for loop is useful for repeated iteration.

## Syntax

```cpp
for (int i = 0; i < n; i++) {
}
```

## Notes

Use `long long` where the iteration variable may exceed `int`.

## Keywords

- for
- forloop
- loop
```

The app should render this into a readable programming-documentation UI.

---

# 10. Markdown Import UX

When the user/admin chooses Markdown creation, provide:

1. Markdown editor
2. Preview
3. Generated hierarchy preview
4. Save/Import button

Example:

```text
Markdown
┌───────────────────────────────┐
│ ## Arrays                     │
│                               │
│ ### Two Pointer               │
│                               │
│ ### Sliding Window            │
└───────────────────────────────┘

Preview
C++
└── DSA
    └── Arrays
        ├── Two Pointer
        └── Sliding Window

[Cancel] [Import]
```

The user should be able to verify the hierarchy before committing the changes.

---

# 11. Home Screen

The Home screen should contain a prominent global search bar at the top.

Placeholder:

```text
Search anything...
```

Below it, show top-level category cards.

Example:

```text
C++
Python
Java
JavaScript
TypeScript
DSA
Databases
Operating Systems
Computer Networks
```

The exact official categories are controlled by the Super Admin.

The Home screen should be useful immediately after installation.

---

# 12. Global Search

The global search must work entirely from local storage.

Search across:

- official Death Code categories
- official subcategories
- official titles
- official Markdown
- official syntax
- official notes
- official keyboard keywords
- user's private content
- user's private notes
- user's private syntax/templates
- downloaded Community content
- Community titles
- Community Markdown
- Community syntax
- Community keywords

Do not make a network call during ordinary search.

Search must work offline.

---

# 13. Search Query Processing

Do **not** use:

- LLM
- RAG
- generative AI
- embedding API
- cloud semantic search

for the core search system.

Use deterministic local search.

Recommended implementation:

```text
User query
    ↓
Normalize
    ↓
Tokenize
    ↓
Remove irrelevant/common terms where appropriate
    ↓
Full-text search
    +
Prefix matching
    +
Fuzzy matching where useful
    ↓
Rank results
    ↓
Display results
```

Example:

```text
How can I convert a decimal number into a binary number?
```

can be normalized into useful terms such as:

```text
decimal
binary
convert
number
```

Search across the indexed fields.

---

# 14. Search Ranking

Rank results using factors such as:

1. exact title match
2. exact keyword match
3. title prefix/partial match
4. exact phrase match
5. syntax match
6. note match
7. Markdown/content match
8. category/subcategory context
9. fuzzy similarity

Return the strongest results first.

Example result:

```text
For Loop

C++ → DSA → Fundamentals → Loops

for (int i = 0; i < n; i++) {
    ...
}

Matched:
for
loop
```

---

# 15. Search Source Filters

Allow the user to control which local data sources are included.

Example:

```text
☑ Death Code
☑ My Notes
☐ Community
```

The search engine must search only selected local sources.

Changing these filters must not require a backend request.

---

# 16. Category/Subcategory Browsing

Every container should show its children clearly.

Use cards, lists, or another highly readable layout.

Show breadcrumbs such as:

```text
C++ > DSA > Fundamentals > Loops > For Loop
```

Navigation must support unlimited nesting.

The UI must remain usable even when the hierarchy becomes deep.

---

# 17. Cards Mode and Reading Mode

Where useful, a container with children must provide two modes.

## Cards Mode

Display child nodes as cards.

Example:

```text
┌────────────────────┐
│ Data Types         │
└────────────────────┘

┌────────────────────┐
│ Loops              │
└────────────────────┘

┌────────────────────┐
│ Functions          │
└────────────────────┘
```

## Reading Mode

Render the relevant Markdown as a continuous document.

Example:

```text
Loops

Loops are used to repeatedly execute a block of code.

For Loop
--------

A for loop is useful when...

Syntax

for (int i = 0; i < n; i++) {
    ...
}

While Loop
----------
...
```

The same underlying content can therefore be browsed hierarchically or read continuously.

---

# 18. Content Cards

A useful programming card should support:

### Title

Example:

```text
For Loop
```

### Syntax

```cpp
for (int i = 0; i < n; i++) {

}
```

### Notes

```text
Use this pattern when iterating over a known range.
```

### Keyboard Keywords

```text
for
forloop
loop
```

Optionally support examples and references.

---

# 19. Syntax Support

Use syntax highlighting for code blocks.

Support common programming languages including:

- C++
- C
- Java
- Python
- JavaScript
- TypeScript
- Go
- Rust
- Kotlin
- Swift
- SQL

Make the architecture extensible so languages can be added later.

---

# 20. Language-Specific Snippets

The same keyword can have different templates for different languages.

Example:

```text
Keyword: bfs

C++:
< C++ BFS template >

Python:
< Python BFS template >

Java:
< Java BFS template >
```

The user can choose the active keyboard language.

The keyboard should use the corresponding template.

---

# 21. Private User Notes

Users can attach private notes to official Death Code content.

Example:

```text
For Loop

Official Syntax:
for (int i = 0; i < n; i++) {}

My Note:
I usually start from 1 in these problems.

My Template:
for (long long i = 0; i < n; i++) {}
```

Private notes remain on the device.

They are not uploaded automatically.

---

# 22. Personal Content Tree

Users must be able to create their own complete content structure.

Example:

```text
My Notes
├── Interview Preparation
│   ├── Arrays
│   ├── Linked Lists
│   ├── Trees
│   └── Graphs
│
└── Templates
    ├── BFS
    ├── DFS
    ├── Dijkstra
    └── Pattern Matching
```

At **every node**, the user can choose:

```text
Add manually
```

or:

```text
Create/import using Markdown
```

This must remain true regardless of nesting depth.

---

# 23. User-Created Content and Keyboard

A user-created card can contain:

- title
- Markdown
- syntax
- notes
- keywords
- language
- placeholders
- reusable template

Example:

```text
Title:
Pattern Matching

Syntax:
for (int i = 1; i <= n; i++) {
    ...
}

Keywords:
triangle
pattern
pyramid
```

When the user types:

```text
triangle
```

the Death Code Keyboard should be able to show that template as a suggestion.

---

# 24. Personal Overrides

Do not allow users to directly overwrite official Death Code content.

Instead, allow private customization associated with official content.

Example:

```text
Official:
C++ → DSA → Loops → For Loop

Official Syntax:
for (int i = 0; i < n; i++) {}

My Note:
Prefer long long here.

My Personal Template:
for (long long i = 0; i < n; i++) {}
```

This preserves the integrity of official content while allowing personalization.

---

# 25. Death Code Keyboard

Build a custom Android keyboard using `InputMethodService`.

Keyboard design:

```text
┌─────────────────────────────────────────────┐
│ For Loop │ For Each │ For In │ While       │
├─────────────────────────────────────────────┤
│ { } ( ) [ ] < > ; : = -> ...               │
├─────────────────────────────────────────────┤
│ Q W E R T Y U I O P                         │
│  A S D F G H J K L                          │
│   Z X C V B N M                             │
└─────────────────────────────────────────────┘
```

The layout must be optimized for mobile programming.

---

# 26. Suggestion Area

The top keyboard row is the suggestion area.

It should display matching snippets based on the text currently being entered.

For example, after typing:

```text
for
```

show:

```text
For Loop
For Each
For In
While
```

Prefer showing the **snippet/template name and preview**, not merely repeating plain words.

Suggestions must come from the local database.

No network request should be required.

---

# 27. Programming Symbol Row

Above QWERTY, provide a horizontally scrollable programming-symbol row.

Example:

```text
{ }   ( )   [ ]   < >   ;   :   =   ->   ...
```

Do not permanently display every possible programming symbol.

Support:

- horizontal scrolling
- grouped symbols
- long press for related symbols

Example long press on `=`:

```text
==
!=
<=
>=
=>
```

Example long press on `(`:

```text
)
[
]
{
}
```

The exact interaction can be refined during implementation.

---

# 28. Full QWERTY

Keep the full alphabet.

Do not sacrifice letters to make space for programming symbols.

Prioritize:

- large keys
- comfortable spacing
- easy thumb reach
- familiar QWERTY positioning
- touch feedback
- responsive input

It should feel familiar like a modern general-purpose keyboard while being optimized for programmers.

---

# 29. Hideable Keyboard Sections

The user must be able to independently hide:

- suggestion area
- symbol row

Possible configurations:

```text
Suggestions ON
Symbols ON
QWERTY
```

```text
Suggestions OFF
Symbols ON
QWERTY
```

```text
Suggestions OFF
Symbols OFF
QWERTY
```

This allows users to maximize vertical typing space.

---

# 30. Keyboard Settings

Provide an accessible settings button on the keyboard.

Include settings such as:

- language
- theme
- suggestion behavior
- suggestion row visibility
- symbol row visibility
- key size
- vibration/haptics
- sound
- other basic keyboard preferences

Settings should be stored locally.

---

# 31. Placeholder-Based Templates

Support optional placeholders.

Example:

```text
for (${initializer}; ${condition}; ${increment}) {
    ${cursor}
}
```

The keyboard should be able to insert the template and place the cursor at useful positions.

At minimum, support:

```text
${cursor}
```

Design the template system so additional named placeholders can be supported.

---

# 32. Keyboard Insertion

Use Android's active `InputConnection`.

Support:

- normal character input
- deletion
- cursor movement
- snippet insertion
- replacement of typed keywords with templates
- text replacement where supported
- cursor placement after snippet insertion
- placeholder navigation where feasible

Third-party editors can expose different input behavior, so use robust standard Android input APIs rather than relying on app-specific injection.

---

# 33. Local-First Runtime Architecture

This rule must be enforced throughout the implementation:

```text
Room
    ↓
Runtime source of truth
    ↓
Search / Browse / Notes / Keyboard
```

The backend is not part of the normal runtime path.

Local operations include:

- search
- browse
- open content
- Markdown rendering
- syntax display
- notes
- personal content
- snippet lookup
- keyword lookup
- keyboard suggestions
- snippet insertion

All should work without the internet.

---

# 34. Official Content Synchronization

Official content is controlled by the Super Admin.

Flow:

```text
Admin creates/edits content
        ↓
Admin previews
        ↓
Admin publishes
        ↓
Backend creates a new published version
        ↓
User app detects newer version
        ↓
App downloads update
        ↓
Validate update
        ↓
Apply to Room
        ↓
Local content is updated
```

The app must not request individual content cards from the backend every time the user opens one.

---

# 35. Versioning

Every published official-content release should have:

- version number
- publish timestamp
- content hash/checksum
- metadata required for synchronization

Example:

```text
Local Version: 41
Server Version: 42
```

The app downloads version 42 and updates the local database.

After synchronization:

```text
Local Version: 42
```

---

# 36. Safe Synchronization

Never destroy a working local version because a new update fails.

Handle:

- network failure
- partial download
- invalid content
- checksum mismatch
- interrupted update
- app crash during synchronization
- database migration problems

Recommended flow:

```text
Current Local Version
        ↓
Download New Package
        ↓
Validate
        ↓
Apply in Transaction
        ↓
Mark New Version Active
```

If validation/application fails:

```text
Keep Current Local Version
```

---

# 37. Initial Content

The application must be usable immediately after installation.

Prefer bundling an initial official content package with the application.

Flow:

```text
Install
 ↓
Initialize Room
 ↓
Insert bundled official content
 ↓
App is immediately usable
 ↓
Check for updates later
```

Do not make the initial app experience depend on a successful network request.

---

# 38. Community

Provide a moderated Community section.

Community is a **curated programming-content library**, not a social network.

Users can submit:

- programming notes
- explanations
- useful code templates
- snippets
- examples
- useful techniques
- other high-quality programming content

Only approved submissions become publicly visible.

---

# 39. Community Authentication

A user needs to sign up/log in only when submitting content to Community.

The user can remain anonymous for normal use.

Community submission flow:

```text
User creates content
        ↓
Login / Sign up
        ↓
Submit
        ↓
Server-side rate limit
        ↓
Pending Review
        ↓
Super Admin
    ├── Approve
    └── Reject
        ↓
Approved Community Content
```

---

# 40. Community Moderation

Super Admin should be able to:

- view pending submissions
- inspect content
- edit content if appropriate
- approve
- reject
- remove previously approved content if necessary

Initial Community features should NOT include:

- messaging
- public chat
- follower systems
- social feeds
- complicated profiles
- direct user-to-user communication
- comments

Keep Community focused on useful programming content.

---

# 41. Community Rate Limiting

Rate limiting must happen server-side.

Protect against:

- spam submissions
- repeated submissions
- large payloads
- excessive API calls
- too many pending submissions
- abuse

Possible limits include:

```text
requests per minute
submissions per hour/day
maximum Markdown size
maximum pending submissions per account
```

Make the limits configurable on the server/admin side.

Do not rely only on Android-side limits because clients can be modified or bypassed.

---

# 42. Community Local Storage

Approved Community content should be synchronized to users and stored locally.

After synchronization:

```text
Browse Community
Search Community
Open Community Content
```

must work offline.

Do not make a request for every individual Community result.

---

# 43. Official and Community Content Distribution

Both official and approved Community content should ultimately reach the local device database.

Conceptually:

```text
                   BACKEND
                      │
           ┌──────────┴──────────┐
           │                     │
      Official Updates     Approved Community
           │                     │
           └──────────┬──────────┘
                      ↓
                Sync Service
                      ↓
                 Room Database
                      ↓
       ┌──────────────┼──────────────┐
       │              │              │
     Search         Browse        Keyboard
```

Once synchronized, the backend is not required for normal access.

---

# 44. Private Data Privacy

Private user content must remain private by default.

Do not automatically upload:

- personal notes
- personal categories
- personal subcategories
- personal snippets
- personal templates
- keyboard keyword mappings

Only content that the user explicitly submits to Community may be sent to the backend.

---

# 45. Backend Responsibilities

Keep backend responsibilities intentionally limited.

## Official Content

The backend should:

- store official content
- store publication metadata
- store content versions
- expose latest version metadata
- provide published content/update packages

## Community

The backend should:

- authenticate Community users
- accept submissions
- rate-limit requests
- store submissions
- store moderation state
- allow admin review
- store approved Community content
- distribute approved content

Do not turn the backend into the runtime search engine.

---

# 46. Database Separation

## Neon PostgreSQL

Store:

```text
Official content
Official content versions
Community users/accounts
Community submissions
Moderation state
Approved Community content
Publishing metadata
Synchronization metadata
```

## Room

Store:

```text
Official synchronized content
Approved Community content
Private user content
Private notes
Personal snippets
Keyboard keyword mappings
Search indexes
Sync metadata
Local preferences
```

The Room database is the runtime source of truth.

---

# 47. Suggested Local Entities

Design appropriate Room entities, potentially including:

```text
ContentNode
ContentDocument
Snippet
PrivateNote
KeyboardKeyword
UserPreference
SearchDocument
SyncMetadata
CommunityContent
```

A hierarchical node should use `parentId` or another robust tree representation.

Use appropriate:

- foreign keys
- indexes
- unique constraints
- transactions

Do not hardcode maximum hierarchy depth.

---

# 48. Search Indexing

Maintain local search indexes for relevant searchable fields.

Index fields such as:

```text
title
slug
markdownContent
syntax
notes
keywords
category names
ancestor/category path
```

When official or Community content is synchronized, update its local indexes.

When private content changes, update its local indexes immediately.

Search should reflect changes without requiring a server call.

---

# 49. Search Result Context

A search result should tell the user where the content is.

Example:

```text
For Loop
C++ → DSA → Fundamentals → Loops

for (int i = 0; i < n; i++) {
    ...
}

Matched in:
Title · Keyword · Syntax
```

For a personal result:

```text
Pattern Matching
My Notes → Templates → Patterns

Matched in:
Note · Keyword
```

---

# 50. Keyboard Suggestion Ranking

Suggestions should be local and fast.

Recommended ranking:

1. exact typed keyword
2. prefix match
3. personal snippets
4. frequently used snippets
5. official Death Code snippets
6. Community snippets

Avoid overwhelming the user with unrelated results.

---

# 51. Content Editing Rules

## Admin

Admin can edit:

- official categories
- official subcategories
- official content
- Markdown
- syntax
- notes
- keywords
- ordering
- structure

Changes become user-visible when published.

## User

User can edit:

- their own private content
- their private notes
- personal snippets
- personal keywords

They cannot modify official source content.

---

# 52. Official Content Publishing

Do not immediately publish every admin save.

Use:

```text
Draft
 ↓
Preview
 ↓
Publish
 ↓
New Content Version
 ↓
Users Synchronize
```

This lets the administrator prepare multiple changes and publish them together.

---

# 53. Moving and Reorganizing Nodes

The admin and user should be able to reorganize content they own.

Support, where practical:

- move node under another node
- reorder siblings
- rename node
- delete node
- duplicate node
- edit Markdown
- edit individual content

When moving a parent node, its descendants must move with it without breaking relationships.

---

# 54. Recursive Content Example

The final system must support a structure such as:

```text
C++
└── DSA
    ├── Fundamentals
    │   ├── Data Types
    │   └── Loops
    │       ├── For Loop
    │       ├── While Loop
    │       └── Do While Loop
    │
    ├── C++ Built-in Functions
    │   ├── Vector
    │   │   ├── push_back()
    │   │   ├── pop_back()
    │   │   └── size()
    │   │
    │   └── Algorithms
    │       ├── sort()
    │       ├── reverse()
    │       ├── lower_bound()
    │       └── upper_bound()
    │
    └── Graphs
        ├── BFS
        └── DFS
```

And every one of these nodes can, where authorized, independently offer:

```text
Manual Create
Markdown Create
Edit
Add Content
```

This recursive behavior is a fundamental requirement.

---

# 55. UI/UX

Use a modern developer-tool visual language.

Default toward a dark programmer-oriented theme, but support multiple themes.

Prioritize:

- readable typography
- excellent code readability
- comfortable spacing
- fast navigation
- minimal clutter
- clear hierarchy
- fast search
- obvious actions
- touch-friendly controls

Avoid excessive animations.

The application should feel fast and technical.

---

# 56. Main Navigation

Use a clean navigation structure such as:

```text
Home
My Notes
Community
Keyboard
Settings
```

The exact navigation pattern can be refined as implementation proceeds.

Content screens should have:

- back navigation
- breadcrumbs
- clear title
- Cards/Reading mode switch where appropriate

---

# 57. Settings

Provide application settings for:

- theme
- language/keyboard language
- content sync behavior
- search preferences where useful
- keyboard preferences
- privacy information
- Community account state
- about/version

The user should be able to understand whether content is local and whether synchronization is currently needed.

---

# 58. Offline Behavior

The application should continue to work normally when offline.

Offline functionality includes:

- browsing installed official content
- searching official content
- browsing personal content
- searching personal content
- reading Markdown
- editing personal notes
- creating personal content
- creating snippets
- keyboard suggestions
- snippet insertion
- keyboard configuration

Only genuinely server-dependent actions should require internet, such as:

- checking for content updates
- downloading new content
- submitting to Community
- authenticating
- retrieving newly approved Community updates

---

# 59. Sync UX

Synchronization should be unobtrusive.

Possible states:

```text
Up to date
```

```text
Update available
```

```text
Updating...
```

```text
Sync failed — will retry later
```

Do not block the application when an update is unavailable.

The existing local content must remain usable.

---

# 60. Performance

Local operations must feel instantaneous.

Optimize:

- search
- category navigation
- Markdown rendering
- syntax highlighting
- keyboard suggestions
- snippet insertion
- local indexing
- synchronization

Do not perform expensive database/search operations on the main UI thread.

Use Kotlin coroutines and appropriate background execution.

---

# 61. Error Handling

Handle gracefully:

- backend unavailable
- no internet
- malformed Markdown
- invalid Markdown hierarchy
- duplicate keywords
- synchronization failure
- corrupted update package
- content validation failure
- Room migration failure
- keyboard not enabled
- unsupported input field
- failed snippet insertion

Never replace valid local content with invalid or incomplete server data.

---

# 62. Security

Implement appropriate backend security.

Important requirements:

- never expose Neon credentials in the Android application
- keep database access behind the backend API
- authenticate protected endpoints
- authorize admin operations server-side
- apply server-side Community rate limits
- validate request payloads
- validate Markdown/content size
- prevent users from modifying official content
- never trust client-side admin flags
- use HTTPS
- safely store authentication tokens on-device

---

# 63. Admin / Super Admin

Create a dedicated protected administrative interface.

Super Admin abilities:

```text
Official Content
├── Create
├── Edit
├── Delete
├── Move
├── Reorder
├── Markdown Import
├── Manual Creation
├── Preview
└── Publish

Community
├── Pending
├── Approved
├── Rejected
├── Review
├── Edit
└── Remove
```

Admin authentication/authorization must be enforced by the backend.

---

# 64. Content Package Design

Design a stable serialization format for synchronized content.

A package should include enough information to reconstruct:

- hierarchy
- Markdown
- syntax
- notes
- keywords
- language
- ordering
- content IDs
- version information

Example conceptual payload:

```text
Content Package
├── packageVersion
├── generatedAt
├── checksum
└── nodes[]
```

The format should be designed so it can later support delta updates without requiring a complete architectural rewrite.

---

# 65. Community Synchronization

When Community content is approved:

```text
Approved
   ↓
Published Community version
   ↓
User checks for updates
   ↓
Download
   ↓
Validate
   ↓
Save locally
   ↓
Searchable offline
```

Community content must not automatically become a live database dependency.

---

# 66. No AI Requirement

The core Death Code product must work without:

- AI
- LLMs
- RAG
- embeddings
- generative code generation

Do not add AI merely because it is technically possible.

The key value comes from:

```text
Structured knowledge
+
User customization
+
Local search
+
Reusable templates
+
Programming keyboard
```

The architecture may remain extensible for future optional features, but the core product must remain fully functional without AI.

---

# 67. No Voice-to-Code Requirement

Do not make voice-to-code a core feature.

The product should remain focused on:

- programming knowledge
- snippets
- notes
- local search
- keyboard productivity

Do not introduce speech-to-code complexity into the core architecture unless explicitly requested later.

---

# 68. Suggested Project Structure

Use clean architecture and clear module boundaries.

A possible structure:

```text
app/
    ui/
    navigation/
    theme/

data/
    local/
        room/
        datastore/
        search/
    remote/
        api/
        dto/
        sync/

domain/
    model/
    repository/
    usecase/

keyboard/
    service/
    ui/
    suggestions/
    snippets/
    input/

admin/
    api/
    models/

community/
    models/
    repository/
```

The exact package/module layout can be adjusted based on project size.

Keep keyboard logic separate enough that it can operate independently from the main Compose UI.

---

# 69. Testing Requirements

Include tests for:

### Local Database

- insert
- update
- delete
- nested hierarchy
- node moving
- transaction safety

### Markdown

- heading parsing
- recursive hierarchy creation
- relative import location
- malformed Markdown handling
- preview correctness

### Search

- exact matches
- prefix matches
- fuzzy matches
- ranking
- notes search
- keyword search
- category search
- source filtering

### Keyboard

- keyword matching
- language-specific snippets
- snippet insertion
- placeholder handling
- cursor placement
- section visibility

### Synchronization

- version comparison
- download validation
- checksum validation
- update transactions
- failed update rollback/preservation
- offline behavior

### Community

- authentication
- rate limits
- submission validation
- admin approval/rejection
- approved content synchronization

---

# 70. Acceptance Criteria

The implementation should not be considered complete unless the following are possible.

### Content Creation

An authorized creator can enter any category/subcategory and choose either:

```text
Create Manually
```

or:

```text
Create using Markdown
```

This must work at unlimited nesting depth.

Example:

```text
C++
 → DSA
   → C++ Built-in Functions
     → Algorithms
       → Sorting
```

At `Sorting`, the creator must still be able to choose:

```text
Manual
```

or:

```text
Markdown
```

### Search

A user can search:

```text
How do I convert decimal to binary?
```

and receive the relevant local card without contacting the backend.

### Personalization

A user can attach a private note and a personal template to official content.

### Keyboard

Typing:

```text
for
```

can produce a snippet suggestion.

Typing a personal keyword such as:

```text
triangle
```

can produce the user's private pattern template.

### Offline

After synchronization, the user can switch off the internet and continue:

```text
Search
Browse
Read
Use snippets
Use keyboard
Edit private notes
```

### Admin

Admin can edit official Markdown and publish a new version.

### Distribution

After publication:

```text
Server Version > Local Version
```

The app downloads the update and stores the new content locally.

### Community

A user can log in only when submitting a Community contribution.

The submission is rate limited and requires admin approval.

---

# 71. Most Important Architectural Principle

The system must follow:

```text
                    BACKEND
                       │
            ┌──────────┴──────────┐
            │                     │
     Official Updates       Community
            │              Submission/Sync
            │                     │
            └──────────┬──────────┘
                       ↓
                Local Room DB
                       │
          ┌────────────┼─────────────┐
          │            │             │
        Search       Content      Keyboard
          │            │             │
          └────────────┴─────────────┘
```

**Room is the runtime source of truth for the Android app.**

The backend is primarily responsible for:

1. distributing official content updates
2. handling Community functionality

---

# 72. Final User Experience

## Official Knowledge

```text
Open Death Code
      ↓
Search / Browse
      ↓
C++
      ↓
DSA
      ↓
Fundamentals
      ↓
Loops
      ↓
For Loop
      ↓
Syntax + Notes + Keyboard Keywords
```

## User Content

```text
My Notes
      ↓
Create Category
      ↓
Create Subcategory
      ↓
Create Another Nested Subcategory
      ↓
Create Content
      ↓
Manual OR Markdown
      ↓
Add Syntax
      ↓
Add Notes
      ↓
Add Keyboard Keywords
      ↓
Saved Locally
      ↓
Immediately Searchable
      ↓
Immediately Available to Keyboard
```

## Admin Content Creation

```text
Admin
 ↓
Choose any official category/subcategory
 ↓
Manual OR Markdown
 ↓
Preview
 ↓
Save Draft
 ↓
Publish
 ↓
New Content Version
 ↓
Users Synchronize
 ↓
Updated Content Stored Locally
```

## Community

```text
User
 ↓
Create useful content
 ↓
Login / Sign up
 ↓
Submit
 ↓
Server-side rate limiting
 ↓
Admin Review
 ↓
Approve
 ↓
Community Publication
 ↓
Users Synchronize
 ↓
Stored Locally
 ↓
Searchable Offline
```

---

# 73. Final Product Definition

Build Death Code as an **offline-first programmable knowledge platform for programmers**.

Its defining characteristics are:

- recursive programming knowledge hierarchy
- manual content creation at any depth
- Markdown content/hierarchy creation at any depth
- official admin-managed knowledge
- completely private local user knowledge
- optional moderated Community contributions
- powerful local search
- reusable syntax/templates
- customizable keyboard keywords
- language-specific snippets
- dedicated programming keyboard
- local-first offline operation
- versioned official content synchronization
- server-side Community moderation and rate limiting
- no mandatory login
- no dependency on AI for core functionality

The application should be architected so that:

> **The user opens Death Code, finds and reads what they need locally, creates and personalizes their own knowledge locally, and uses the same knowledge directly from the programming keyboard.**

The backend should remain mostly invisible to the normal user experience and should only become relevant when content synchronization or Community functionality actually requires it.
