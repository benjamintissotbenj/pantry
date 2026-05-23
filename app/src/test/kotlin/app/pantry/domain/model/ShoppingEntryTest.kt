package app.pantry.domain.model

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShoppingEntryTest {
    @Test
    fun `auto entry surfaces source and catalog hints`() {
        val now = Instant.parse("2026-05-23T10:00:00Z")
        val entry = ShoppingEntry(
            id = "auto:abc",
            name = "Milk",
            source = ShoppingEntry.Source.AUTO,
            checked = false,
            createdAt = now,
            linkedItemId = "abc",
            category = "Dairy",
            currentQuantity = 0.0,
            threshold = 2.0,
            defaultRestockQuantity = 4.0,
        )
        assertEquals(ShoppingEntry.Source.AUTO, entry.source)
        assertEquals("abc", entry.linkedItemId)
        assertEquals(4.0, entry.defaultRestockQuantity)
    }

    @Test
    fun `manual ad-hoc entry has no link or catalog hint`() {
        val entry = ShoppingEntry(
            id = "entry1",
            name = "Wine for guests",
            source = ShoppingEntry.Source.MANUAL,
            checked = false,
            createdAt = Instant.now(),
            linkedItemId = null,
            category = "Other",
            currentQuantity = null,
            threshold = null,
            defaultRestockQuantity = null,
        )
        assertNull(entry.linkedItemId)
        assertNull(entry.threshold)
        assertEquals("Other", entry.category)
    }
}
