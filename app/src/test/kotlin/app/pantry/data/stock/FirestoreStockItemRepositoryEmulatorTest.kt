package app.pantry.data.stock

import app.cash.turbine.test
import app.pantry.domain.model.StockUnit
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Tag
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Tag("emulator")
class FirestoreStockItemRepositoryEmulatorTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: FirestoreStockItemRepository

    @Before
    fun setUp() {
        assumeTrue(
            "Skipping emulator test — pass -PincludeEmulatorTests with the emulator running",
            System.getProperty("includeEmulatorTests") != null,
        )
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(
                ctx,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("pantry-dev-1922e")
                    .setApiKey("fake-api-key")
                    .build(),
            )
        }
        firestore = FirebaseFirestore.getInstance()
        if (!emulatorConfigured) {
            firestore.useEmulator("localhost", 8080)
            emulatorConfigured = true
        }
        repo = FirestoreStockItemRepository(firestore)
    }

    @Test
    fun `create observe increment delete round-trip`() = runTest {
        val householdId = "test-hh-${System.currentTimeMillis()}"

        val created = repo.create(
            householdId = householdId,
            name = "Milk",
            category = "Fridge",
            unit = StockUnit.LITER,
            quantity = 1.0,
            threshold = 1.0,
        )
        assertNotNull(created.getOrNull())
        val itemId = created.getOrThrow().id

        repo.observe(householdId).test {
            // First emission: contains the created item
            var items = awaitItem()
            while (items.none { it.id == itemId }) items = awaitItem()
            assertEquals("Milk", items.first { it.id == itemId }.name)

            // Increment by +1
            repo.adjustQuantity(householdId, itemId, 1.0).getOrThrow()
            var updated = awaitItem()
            while (updated.first { it.id == itemId }.quantity < 2.0) updated = awaitItem()
            assertEquals(2.0, updated.first { it.id == itemId }.quantity, 0.001)

            // Delete
            repo.delete(householdId, itemId).getOrThrow()
            var afterDelete = awaitItem()
            while (afterDelete.any { it.id == itemId }) afterDelete = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private var emulatorConfigured = false
    }
}
