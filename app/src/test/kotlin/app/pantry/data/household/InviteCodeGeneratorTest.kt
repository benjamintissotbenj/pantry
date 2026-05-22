package app.pantry.data.household

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class InviteCodeGeneratorTest {
    @Test
    fun `generates a 6-character uppercase alphanumeric code`() {
        val code = InviteCodeGenerator(random = Random(seed = 42)).next()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isUpperCase() || it.isDigit() })
    }

    @Test
    fun `consecutive calls produce different codes`() {
        val gen = InviteCodeGenerator(random = Random(seed = 42))
        val a = gen.next()
        val b = gen.next()
        assertTrue(a != b)
    }
}
