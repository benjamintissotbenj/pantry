package app.pantry.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pantry.BuildConfig
import app.pantry.data.auth.AuthRepository
import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.household.CurrentHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.stock.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
    private val stock: StockItemRepository,
    private val connectivity: ConnectivityRepository,
    private val currentHousehold: CurrentHouseholdRepository,
) : ViewModel() {

    private val events = MutableStateFlow(EventState())

    private data class EventState(
        val snackbar: String? = null,
        val clipboard: String? = null,
        val share: String? = null,
        val postLeaveNav: Boolean = false,
        val signedOut: Boolean = false,
    )

    val uiState: StateFlow<SettingsUiState> =
        currentHousehold.currentHouseholdId
            .flatMapLatest { hid ->
                if (hid == null) flowOf(SettingsUiState())
                else combine(
                    households.observe(hid),
                    auth.currentUser,
                    stock.observe(hid),
                    connectivity.isOffline,
                    events,
                ) { hh, user, items, offline, ev ->
                    project(hid, hh, user, items, offline, ev)
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private fun project(
        hid: String,
        hh: app.pantry.domain.model.Household?,
        user: app.pantry.domain.model.UserProfile?,
        items: List<app.pantry.domain.model.StockItem>,
        offline: Boolean,
        ev: EventState,
    ): SettingsUiState {
        val uid = user?.uid.orEmpty()
        val isCreator = hh?.createdBy == uid && uid.isNotEmpty()
        val members = hh?.memberUids.orEmpty().map { memberUid ->
            val summary = hh?.members?.get(memberUid)
            MemberRow(
                uid = memberUid,
                displayName = summary?.displayName ?: "—",
                email = summary?.email ?: "",
                isYou = memberUid == uid,
                canRemove = isCreator && memberUid != uid,
            )
        }
        val categories = items
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
        return SettingsUiState(
            isOffline = offline,
            householdId = hid,
            householdName = hh?.name.orEmpty(),
            inviteCode = hh?.inviteCode.orEmpty(),
            currentUserUid = uid,
            isCreator = isCreator,
            members = members,
            categories = categories,
            appVersion = BuildConfig.VERSION_NAME,
            pendingSnackbar = ev.snackbar,
            pendingClipboard = ev.clipboard,
            pendingShareCode = ev.share,
            pendingPostLeaveNav = ev.postLeaveNav,
            signedOut = ev.signedOut,
        )
    }

    fun onRenameHousehold(newName: String) {
        val hid = uiState.value.householdId ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            households.rename(hid, trimmed).fold(
                onSuccess = { events.update { it.copy(snackbar = "Household renamed") } },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't rename") } },
            )
        }
    }

    fun onCopyCodeRequested() {
        val code = uiState.value.inviteCode
        if (code.isNotEmpty()) events.update { it.copy(clipboard = code, snackbar = "Invite code copied") }
    }

    fun onShareCodeRequested() {
        val code = uiState.value.inviteCode
        if (code.isNotEmpty()) events.update { it.copy(share = code) }
    }

    fun onRegenerateCode() {
        val hid = uiState.value.householdId ?: return
        viewModelScope.launch {
            households.regenerateInviteCode(hid).fold(
                onSuccess = { events.update { it.copy(snackbar = "Code changed") } },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't change code") } },
            )
        }
    }

    fun onRemoveMember(uid: String) {
        val hid = uiState.value.householdId ?: return
        val name = uiState.value.members.firstOrNull { it.uid == uid }?.displayName
        viewModelScope.launch {
            households.removeMember(hid, uid).fold(
                onSuccess = {
                    val msg = if (name.isNullOrBlank()) "Member removed" else "Removed $name"
                    events.update { it.copy(snackbar = msg) }
                },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't remove member") } },
            )
        }
    }

    fun onRenameCategory(oldName: String, newName: String) {
        val hid = uiState.value.householdId ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == oldName) return
        viewModelScope.launch {
            households.renameCategory(hid, oldName, trimmed).fold(
                onSuccess = { count -> events.update { it.copy(snackbar = "Renamed $count item(s)") } },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't rename category") } },
            )
        }
    }

    fun onLeaveHousehold() {
        val hid = uiState.value.householdId ?: return
        viewModelScope.launch {
            households.leaveHousehold(hid).fold(
                onSuccess = { events.update { it.copy(postLeaveNav = true) } },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't leave household") } },
            )
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            auth.signOut().fold(
                onSuccess = { events.update { it.copy(signedOut = true) } },
                onFailure = { err -> events.update { it.copy(snackbar = err.message ?: "Couldn't sign out") } },
            )
        }
    }

    fun consumeSnackbar() { events.update { it.copy(snackbar = null) } }
    fun consumeClipboard() { events.update { it.copy(clipboard = null) } }
    fun consumeShareCode() { events.update { it.copy(share = null) } }
    fun consumeNav() { events.update { it.copy(postLeaveNav = false) } }
    fun consumeSignedOut() { events.update { it.copy(signedOut = false) } }
}
