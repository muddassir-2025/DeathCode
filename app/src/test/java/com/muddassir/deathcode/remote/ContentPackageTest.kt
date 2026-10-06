package com.muddassir.deathcode.remote

import com.muddassir.deathcode.data.remote.ContentChecksum
import com.muddassir.deathcode.data.remote.ContentPackageValidator
import com.muddassir.deathcode.data.remote.dto.ContentNodeDto
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackageTest {

    private fun node(id: String, parentId: String? = null, order: Int = 0, title: String = id) =
        ContentNodeDto(id = id, parentId = parentId, title = title, sortOrder = order)

    private fun pkg(nodes: List<ContentNodeDto>, version: Long = 2) =
        ContentPackageDto(
            packageVersion = version,
            checksum = ContentChecksum.compute(nodes),
            nodes = nodes,
        )

    // ---------------------------------------------------------------- checksum

    /**
     * Cross-language contract test.
     *
     * `backend/src/packages.js` computes the same canonical form; the value below is the
     * output of the Node implementation for the same four fields. If either side drifts,
     * every download would be rejected as "checksum mismatch", so this is pinned.
     */
    @Test
    fun `checksum matches the server implementation`() {
        val nodes = listOf(
            ContentNodeDto(
                id = "b",
                parentId = "a",
                title = "Child",
                slug = "child",
                markdown = "line1\nline2",
                syntax = "int x;",
                notes = null,
                language = "cpp",
                keywords = listOf("for", "loop"),
                sortOrder = 1,
            ),
            ContentNodeDto(
                id = "a",
                parentId = null,
                title = "Root",
                slug = "root",
                markdown = "",
                syntax = null,
                notes = "note",
                language = null,
                keywords = emptyList(),
                sortOrder = 0,
            ),
        )

        assertEquals(
            "8374ae01cc215664415802d77b2488f265a0835b4a2afc24168537a7bda64a70",
            ContentChecksum.compute(nodes),
        )
    }

    @Test
    fun `checksum is deterministic and order independent`() {
        val a = listOf(node("a"), node("b"))
        val b = listOf(node("b"), node("a"))

        assertEquals(ContentChecksum.compute(a), ContentChecksum.compute(b))
    }

    @Test
    fun `checksum changes when content changes`() {
        val before = ContentChecksum.compute(listOf(node("a", title = "One")))
        val after = ContentChecksum.compute(listOf(node("a", title = "Two")))

        assertTrue(before != after)
    }

    @Test
    fun `package with a matching checksum is intact`() {
        assertTrue(ContentChecksum.isIntact(pkg(listOf(node("a")))))
    }

    @Test
    fun `package with a tampered checksum is rejected`() {
        val tampered = pkg(listOf(node("a"))).copy(checksum = "deadbeef")

        assertFalse(ContentChecksum.isIntact(tampered))
        val result = ContentPackageValidator.validate(tampered)
        assertFalse(result.valid)
        assertEquals("Checksum mismatch", result.reason)
    }

    // ---------------------------------------------------------------- ordering

    @Test
    fun `orders parents before children regardless of input order`() {
        val nodes = listOf(
            node("grandchild", "child", 0),
            node("child", "root", 0),
            node("root", null, 0),
        )

        val ordered = ContentPackageValidator.orderParentsFirst(nodes)!!

        assertEquals(listOf("root", "child", "grandchild"), ordered.map { it.id })
    }

    @Test
    fun `keeps declared sibling order`() {
        val nodes = listOf(
            node("c", "root", 2),
            node("a", "root", 0),
            node("b", "root", 1),
            node("root", null, 0),
        )

        val ordered = ContentPackageValidator.orderParentsFirst(nodes)!!

        assertEquals(listOf("root", "a", "b", "c"), ordered.map { it.id })
    }

    @Test
    fun `rejects a dangling parent reference in a full snapshot`() {
        val nodes = listOf(node("orphan", "missing"))

        assertNull(ContentPackageValidator.orderParentsFirst(nodes))
        val result = ContentPackageValidator.validate(pkg(nodes))
        assertFalse(result.valid)
        assertTrue(result.reason!!.contains("inconsistent parent"))
    }

    @Test
    fun `a partial package may attach to content the device already has`() {
        val nodes = listOf(node("new-node", "existing-parent"))
        val partial = pkg(nodes).copy(fullSnapshot = false)

        val ordered = ContentPackageValidator.orderParentsFirst(nodes, requireParents = false)
        assertNotNull(ordered)
        assertEquals("new-node", ordered!!.single().id)
        assertTrue(ContentPackageValidator.validate(partial).valid)

        // The same nodes are still rejected when declared as a complete snapshot.
        assertFalse(ContentPackageValidator.validate(pkg(nodes)).valid)
    }

    @Test
    fun `a partial package still cannot contain a cycle`() {
        val partial = pkg(listOf(node("a", "b"), node("b", "a")))
            .copy(fullSnapshot = false)

        assertFalse(ContentPackageValidator.validate(partial).valid)
    }

    @Test
    fun `rejects a cycle`() {
        val nodes = listOf(
            node("a", "b"),
            node("b", "a"),
        )

        assertNull(ContentPackageValidator.orderParentsFirst(nodes))
    }

    @Test
    fun `rejects duplicate ids`() {
        val nodes = listOf(node("a"), node("a"))

        assertNull(ContentPackageValidator.orderParentsFirst(nodes))
    }

    @Test
    fun `rejects an empty package`() {
        val result = ContentPackageValidator.validate(pkg(emptyList()))

        assertFalse(result.valid)
        assertEquals("Package contained no content", result.reason)
    }

    @Test
    fun `accepts a deep but consistent hierarchy`() {
        val nodes = mutableListOf(node("n0", null))
        for (i in 1..200) {
            nodes += node("n$i", "n${i - 1}")
        }

        val ordered = ContentPackageValidator.orderParentsFirst(nodes)

        assertNotNull(ordered)
        assertEquals(201, ordered!!.size)
        assertEquals("n0", ordered.first().id)
        assertEquals("n200", ordered.last().id)
    }
}
