package app.pantry.data.household

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Tag
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration test for [FirestoreHouseholdRepository] against the local Firestore emulator.
 *
 * Runs only when the Gradle property `includeEmulatorTests` is present.
 * Start the emulator first: `firebase emulators:start --only firestore`
 * Then run: `./gradlew :app:test --tests "app.pantry.data.household.*" -PincludeEmulatorTests`
 */
@Tag("emulator")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreHouseholdRepositoryEmulatorTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: FirestoreHouseholdRepository

    @Before
    fun setUp() {
        assumeTrue(
            "Skipping emulator test — start the Firebase emulator and pass -PincludeEmulatorTests",
            System.getProperty("includeEmulatorTests") != null,
        )
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(
                ctx,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("pantry-dev")
                    .setApiKey("fake-api-key")
                    .build(),
            )
        }
        if (!emulatorConfigured) {
            FirebaseFirestore.getInstance().useEmulator("localhost", 8080)
            emulatorConfigured = true
        }
        firestore = FirebaseFirestore.getInstance()
        repo = FirestoreHouseholdRepository(firestore, InviteCodeGenerator(kotlin.random.Random.Default))
    }

    @Test
    fun `create then observe round-trip`() = runTest {
        val result = repo.create("Alice's House", ownerUid = "u-1")
        assertTrue(result.isSuccess)
        val household = result.getOrThrow()

        repo.observe(household.id).test {
            val first = awaitItem()
            assertEquals("Alice's House", first?.name)
            assertEquals(listOf("u-1"), first?.memberUids)
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private var emulatorConfigured = false
    }
}
