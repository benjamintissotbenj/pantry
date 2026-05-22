package app.pantry.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

object EmulatorInitializer {
    private const val EMULATOR_HOST = "10.0.2.2" // Android emulator -> host loopback
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080
    private const val FUNCTIONS_PORT = 5001

    fun initialise() {
        FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_PORT)
        FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        FirebaseFunctions.getInstance("europe-west1").useEmulator(EMULATOR_HOST, FUNCTIONS_PORT)
    }
}
