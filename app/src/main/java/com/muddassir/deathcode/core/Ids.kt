package com.muddassir.deathcode.core

import java.util.UUID

/** Generates a locally unique identifier. The prefix makes debugging stored rows easier. */
fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

/**
 * Turns a title into a stable, URL-ish slug.
 * Non alphanumeric characters collapse into single dashes.
 */
fun slugify(title: String): String {
    val sb = StringBuilder(title.length)
    var lastWasDash = false
    for (ch in title.trim().lowercase()) {
        when {
            ch.isLetterOrDigit() -> {
                sb.append(ch)
                lastWasDash = false
            }
            !lastWasDash && sb.isNotEmpty() -> {
                sb.append('-')
                lastWasDash = true
            }
        }
    }
    return sb.toString().trim('-').ifEmpty { "node" }
}
