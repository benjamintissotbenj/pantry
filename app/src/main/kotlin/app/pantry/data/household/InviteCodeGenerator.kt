package app.pantry.data.household

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class InviteCodeGenerator @Inject constructor(
    private val random: Random,
) {
    private val alphabet = ('A'..'Z') + ('0'..'9')

    fun next(length: Int = CODE_LENGTH): String =
        buildString(length) { repeat(length) { append(alphabet.random(random)) } }

    companion object { const val CODE_LENGTH = 6 }
}
