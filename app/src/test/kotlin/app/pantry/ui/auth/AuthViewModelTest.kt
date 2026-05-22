package app.pantry.ui.auth

import app.cash.turbine.test
import app.pantry.data.auth.AuthRepository
import app.pantry.domain.model.AuthError
import app.pantry.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val repo: AuthRepository = mockk(relaxed = true)
    private lateinit var vm: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // currentUser StateFlow must be present
        io.mockk.every { repo.currentUser } returns MutableStateFlow(null)
        vm = AuthViewModel(repo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `email format validation rejects invalid email`() = runTest {
        vm.onEmailChange("not-an-email")
        assertEquals("Enter a valid email address", vm.uiState.value.emailError)
    }

    @Test
    fun `successful sign-in transitions to navigateToHousehold = true`() = runTest {
        coEvery { repo.signInWithEmail("a@b.com", "secret123") } returns
            Result.success(UserProfile("u-1", "Alice", "a@b.com"))

        vm.uiState.test {
            awaitItem() // initial
            vm.switchMode(AuthUiState.Mode.EmailSignIn)
            vm.onEmailChange("a@b.com")
            vm.onPasswordChange("secret123")
            // drain items until canSubmit is true
            var s = awaitItem()
            while (!s.canSubmit) s = awaitItem()
            vm.submit()
            // drain to the "navigate" state
            var state = awaitItem()
            while (!state.navigateToHousehold && state.toastMessage == null) {
                state = awaitItem()
            }
            assertTrue(state.navigateToHousehold)
            assertFalse(state.isSubmitting)
        }
    }

    @Test
    fun `invalid credentials sets toastMessage and stops submitting`() = runTest {
        coEvery { repo.signInWithEmail(any(), any()) } returns Result.failure(AuthError.InvalidCredentials)
        vm.switchMode(AuthUiState.Mode.EmailSignIn)
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("nope01")
        vm.submit()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("Email or password is incorrect", state.toastMessage)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `successful sign-up transitions to navigateToHousehold = true`() = runTest {
        coEvery { repo.signUpWithEmail("a@b.com", "secret123", "Alice") } returns
            Result.success(UserProfile("u-1", "Alice", "a@b.com"))

        vm.switchMode(AuthUiState.Mode.EmailSignUp)
        vm.onDisplayNameChange("Alice")
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("secret123")
        vm.submit()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.navigateToHousehold)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `sign-up cannot submit while displayName is blank`() = runTest {
        vm.switchMode(AuthUiState.Mode.EmailSignUp)
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("secret123")
        // displayName never set — still blank
        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `EmailAlreadyInUse sets toastMessage`() = runTest {
        coEvery { repo.signUpWithEmail(any(), any(), any()) } returns
            Result.failure(AuthError.EmailAlreadyInUse)

        vm.switchMode(AuthUiState.Mode.EmailSignUp)
        vm.onDisplayNameChange("Alice")
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("secret123")
        vm.submit()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("That email is already registered", state.toastMessage)
        assertFalse(state.isSubmitting)
    }
}
