package app.pantry.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class StockItemTest {

    private val now = Instant.parse("2026-06-01T10:00:00Z")

    @Test
    fun `isLowStock true when quantity below threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 0.5, 1.0, now, null)
        assertTrue(item.isLowStock)
    }

    @Test
    fun `isLowStock false when quantity equals threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 1.0, 1.0, now, null)
        assertFalse(item.isLowStock)
    }

    @Test
    fun `isLowStock false when quantity above threshold`() {
        val item = StockItem("i-1", "Milk", "Fridge", StockUnit.LITER, 2.0, 1.0, now, null)
        assertFalse(item.isLowStock)
    }

    @Test
    fun `StockUnit fromStorageKey defaults to COUNT for unknown input`() {
        assertEquals(StockUnit.COUNT, StockUnit.fromStorageKey("widget"))
        assertEquals(StockUnit.COUNT, StockUnit.fromStorageKey(null))
        assertEquals(StockUnit.LITER, StockUnit.fromStorageKey("L"))
    }
}
