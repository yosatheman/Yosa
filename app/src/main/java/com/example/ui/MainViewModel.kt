package com.example.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeepCurrentApp
import com.example.data.DefaultProfiles
import com.example.data.LogEntryEntity
import com.example.data.ProfileEntity
import com.example.data.SessionHistoryEntity
import com.example.data.TunnelRepository
import com.example.tunnel.ConfigParser
import com.example.tunnel.LatencyResult
import com.example.tunnel.TunnelEngine
import com.example.tunnel.TunnelState
import com.example.ui.theme.AccentChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    HOME, PROFILES, LOGS, SETTINGS
}

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val icon: Drawable? = null
)

class MainViewModel : ViewModel() {

    private val repository = TunnelRepository(DeepCurrentApp.instance.database.tunnelDao())
    val engine = TunnelEngine.instance

    // Navigation & Sheet state
    val currentTab = MutableStateFlow(MainTab.HOME)
    val editingProfile = MutableStateFlow<ProfileEntity?>(null)
    val isCreatingNewProfile = MutableStateFlow(false)

    val isProfilePickerOpen = MutableStateFlow(false)
    val isServerDetailOpen = MutableStateFlow(false)
    val profileContextMenuTarget = MutableStateFlow<ProfileEntity?>(null)
    val qrShareProfile = MutableStateFlow<ProfileEntity?>(null)
    val isSpeedTestOpen = MutableStateFlow(false)
    val isPayloadLibraryOpen = MutableStateFlow(false)
    val deleteConfirmProfile = MutableStateFlow<ProfileEntity?>(null)
    val isImportConfigOpen = MutableStateFlow(false)
    val isSessionHistoryOpen = MutableStateFlow(false)
    val showDiscardConfirm = MutableStateFlow(false)

    // Appearance & Settings
    val accentChoice = MutableStateFlow(AccentChoice.CYAN)
    val fontScale = MutableStateFlow(1.0f) // 0.9f, 1.0f, 1.15f
    val themeMode = MutableStateFlow("Dark") // Dark, Light, System
    val onboardingCompleted = MutableStateFlow(true) // User prompt allows skippable, default true or show on fresh run

    // Profiles
    val profileSearchQuery = MutableStateFlow("")
    val allProfiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProfiles: StateFlow<List<ProfileEntity>> = combine(allProfiles, profileSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.host.contains(query, ignoreCase = true) ||
            it.transport.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = repository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Logs & filter
    val logFilter = MutableStateFlow("All") // All, Error, Warn, Info, Debug
    val autoScrollLogs = MutableStateFlow(true)
    private val rawLogs: StateFlow<List<LogEntryEntity>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLogs: StateFlow<List<LogEntryEntity>> = combine(rawLogs, logFilter) { list, filter ->
        when (filter) {
            "Error" -> list.filter { it.level == "ERR" }
            "Warn" -> list.filter { it.level == "WRN" }
            "Info" -> list.filter { it.level == "INF" }
            "Debug" -> list.filter { it.level == "DBG" }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sessions
    val sessionHistory: StateFlow<List<SessionHistoryEntity>> = repository.sessionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Installed apps for Split Tunneling
    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val installedApps: StateFlow<List<AppInfoItem>> = _installedApps.asStateFlow()

    // Transient latency test status for profile editor / card
    val testingLatencyProfileId = MutableStateFlow<Long?>(null)
    val editorTestLatencyResult = MutableStateFlow<LatencyResult?>(null)
    val isTestingEditorConnection = MutableStateFlow(false)

    // Snackbar event
    val snackbarMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.activeProfile.collect { prof ->
                engine.setActiveProfile(prof)
            }
        }
    }

    fun toggleConnect(context: Context) {
        engine.toggleConnect(context)
    }

    fun setActiveProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.setActiveProfile(profile.id)
            engine.setActiveProfile(profile)
            snackbarMessage.value = "Active profile set to ${profile.name}"
        }
    }

    fun startNewProfile() {
        val newProfile = ProfileEntity(
            name = "New Profile",
            transport = "SSL",
            host = "127.0.0.1",
            port = 443,
            flagEmoji = "⚡"
        )
        editingProfile.value = newProfile
        isCreatingNewProfile.value = true
    }

    fun startEditProfile(profile: ProfileEntity) {
        editingProfile.value = profile
        isCreatingNewProfile.value = false
    }

    fun saveEditingProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (isCreatingNewProfile.value) {
                val newId = repository.insertProfile(profile)
                if (allProfiles.value.isEmpty()) {
                    repository.setActiveProfile(newId)
                }
                snackbarMessage.value = "Profile created: ${profile.name}"
            } else {
                repository.updateProfile(profile)
                if (profile.isActive) {
                    engine.setActiveProfile(profile)
                }
                snackbarMessage.value = "Profile saved"
            }
            editingProfile.value = null
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (profile.isActive) {
                val remaining = allProfiles.value.filter { it.id != profile.id }
                remaining.firstOrNull()?.let { repository.setActiveProfile(it.id) }
            }
            snackbarMessage.value = "Deleted profile ${profile.name}"
        }
    }

    fun duplicateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.duplicateProfile(profile)
            snackbarMessage.value = "Duplicated ${profile.name}"
        }
    }

    fun testProfileLatency(profile: ProfileEntity) {
        viewModelScope.launch {
            testingLatencyProfileId.value = profile.id
            val res = engine.testLatency(profile.host, profile.port)
            repository.updateProfile(
                profile.copy(
                    lastTestedLatencyMin = res.min,
                    lastTestedLatencyAvg = res.avg,
                    lastTestedLatencyMax = res.max
                )
            )
            testingLatencyProfileId.value = null
        }
    }

    fun testEditorConnection(host: String, port: Int) {
        viewModelScope.launch {
            isTestingEditorConnection.value = true
            editorTestLatencyResult.value = null
            val res = engine.testLatency(host, port)
            editorTestLatencyResult.value = res
            isTestingEditorConnection.value = false
        }
    }

    fun importConfig(rawText: String): Boolean {
        val parsed = ConfigParser.parse(rawText) ?: return false
        viewModelScope.launch {
            repository.insertProfile(parsed)
            snackbarMessage.value = "Imported config: ${parsed.name}"
        }
        return true
    }

    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = packages.map { appInfo ->
                AppInfoItem(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                )
            }.sortedBy { it.appName.lowercase() }
            _installedApps.value = list
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            snackbarMessage.value = "Logs cleared"
        }
    }

    fun clearSessions() {
        viewModelScope.launch {
            repository.clearAllSessions()
            snackbarMessage.value = "Session history cleared"
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            repository.resetAll()
            snackbarMessage.value = "App reset to factory defaults"
        }
    }
}
