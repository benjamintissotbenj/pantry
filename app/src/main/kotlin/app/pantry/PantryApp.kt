package app.pantry

import android.app.Application
import app.pantry.data.firebase.EmulatorInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PantryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATOR) {
            EmulatorInitializer.initialise()
        }
    }
}
