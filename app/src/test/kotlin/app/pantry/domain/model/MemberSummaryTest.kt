package app.pantry.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberSummaryTest {
    @Test
    fun `member summary holds display name and email`() {
        val m = MemberSummary(displayName = "Ben", email = "ben@example.com")
        assertEquals("Ben", m.displayName)
        assertEquals("ben@example.com", m.email)
    }

    @Test
    fun `household carries createdBy and members map`() {
        val hh = Household(
            id = "hh1",
            name = "Smith family",
            memberUids = listOf("u1", "u2"),
            inviteCode = "ABCDEF",
            createdBy = "u1",
            members = mapOf(
                "u1" to MemberSummary("Ben", "ben@example.com"),
                "u2" to MemberSummary("Alice", "alice@example.com"),
            ),
        )
        assertEquals("u1", hh.createdBy)
        assertEquals(2, hh.members.size)
        assertEquals("Alice", hh.members["u2"]?.displayName)
    }
}
