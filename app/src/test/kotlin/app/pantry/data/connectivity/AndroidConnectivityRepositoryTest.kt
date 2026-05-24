package app.pantry.data.connectivity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidConnectivityRepositoryTest {

    @Test
    fun `flow can be collected without crashing`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AndroidConnectivityRepository(context)
        repo.isOffline.test {
            // Just need to verify a value emits.
            awaitItem()
            cancelAndConsumeRemainingEvents()
        }
    }
}
