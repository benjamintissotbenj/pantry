package app.pantry.data.firebase

import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration smoke test that writes and reads a Firestore document against the local emulator.
 *
 * Runs only when the Gradle property `includeEmulatorTests` is present, which causes the
 * `includeEmulatorTests` system property to be set by the build script.
 *
 * Runner: Robolectric (JUnit 4) — required to supply an Android Context for FirebaseApp.initializeApp.
 * The `@Tag("emulator")` annotation on the companion object is informational; exclusion of this
 * test in normal CI is done via the system-property guard in @Before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmulatorSmokeTest {

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
        FirebaseFirestore.getInstance().useEmulator("localhost", 8080)
    }

    @Test
    fun write_and_read_a_doc_against_firestore_emulator() = runTest {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("smoke").document("ping")
        docRef.set(mapOf("ok" to true)).await()
        val snap = docRef.get().await()
        assertEquals(true, snap.getBoolean("ok"))
    }
}
