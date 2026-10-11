package com.vesper.flipper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.flipper.ble.FlipperFileSystem
import com.vesper.flipper.domain.catalog.FlipperCatalogClient
import com.vesper.flipper.domain.catalog.FlipperCatalogJson
import com.vesper.flipper.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class FapHubViewModel @Inject constructor(
    private val flipperFileSystem: FlipperFileSystem
) : ViewModel() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val catalogClient = FlipperCatalogClient(httpClient)

    // ═══════════════════════════════════════════════════════
    // TAB STATE (Apps vs Resources)
    // ═══════════════════════════════════════════════════════

    enum class HubTab { APPS, RESOURCES }

    private val _activeTab = MutableStateFlow(HubTab.APPS)
    val activeTab: StateFlow<HubTab> = _activeTab.asStateFlow()

    fun setTab(tab: HubTab) { _activeTab.value = tab }

    // ═══════════════════════════════════════════════════════
    // APPS TAB STATE (existing)
    // ═══════════════════════════════════════════════════════

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FapCategory?>(null)
    val selectedCategory: StateFlow<FapCategory?> = _selectedCategory.asStateFlow()

    private val _selectedPack = MutableStateFlow<FapPack?>(null)
    val selectedPack: StateFlow<FapPack?> = _selectedPack.asStateFlow()

    private val _packProgress = MutableStateFlow<String?>(null)
    val packProgress: StateFlow<String?> = _packProgress.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.DOWNLOADS)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _installedApps = MutableStateFlow<Set<String>>(emptySet())
    val installedApps: StateFlow<Set<String>> = _installedApps.asStateFlow()

    private val _installStatus = MutableStateFlow<Map<String, InstallStatus>>(emptyMap())
    val installStatus: StateFlow<Map<String, InstallStatus>> = _installStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedApp = MutableStateFlow<FapApp?>(null)
    val selectedApp: StateFlow<FapApp?> = _selectedApp.asStateFlow()

    val displayedApps: StateFlow<List<FapApp>> = combine(
        _searchQuery, _selectedCategory, _selectedPack, _sortBy, _installedApps
    ) { query, category, pack, sort, installed ->
        var apps = if (pack != null) FapHubCatalog.appsInPack(pack) else FapHubCatalog.allApps
        if (category != null) apps = apps.filter { it.category == category }
        if (query.isNotEmpty()) {
            apps = FapHubCatalog.searchApps(query).let { results ->
                results.filter { app ->
                    (pack == null || app.pack == pack) && (category == null || app.category == category)
                }
            }
        }
        apps = apps.map { it.copy(isInstalled = installed.contains(it.id) || installed.contains(it.catalogAlias)) }
        when (sort) {
            SortOption.DOWNLOADS -> apps.sortedByDescending { it.downloads }
            SortOption.RATING -> apps.sortedByDescending { it.rating }
            SortOption.NAME -> apps.sortedBy { it.name }
            SortOption.UPDATED -> apps.sortedByDescending { it.updatedAt }
            SortOption.CATEGORY -> apps.sortedBy { it.category.displayName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FapHubCatalog.allApps)

    val categoryCounts: StateFlow<Map<FapCategory, Int>> = flow {
        emit(FapCategory.entries.associateWith { FapHubCatalog.getAppsByCategory(it).size })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ═══════════════════════════════════════════════════════
    // RESOURCES TAB STATE (new)
    // ═══════════════════════════════════════════════════════

    private val _resourceSearchQuery = MutableStateFlow("")
    val resourceSearchQuery: StateFlow<String> = _resourceSearchQuery.asStateFlow()

    private val _selectedResourceType = MutableStateFlow<FlipperResourceType?>(null)
    val selectedResourceType: StateFlow<FlipperResourceType?> = _selectedResourceType.asStateFlow()

    private val _selectedRepo = MutableStateFlow<FlipperResourceRepo?>(null)
    val selectedRepo: StateFlow<FlipperResourceRepo?> = _selectedRepo.asStateFlow()

    val displayedResources: StateFlow<List<FlipperResourceRepo>> = combine(
        _resourceSearchQuery, _selectedResourceType
    ) { query, type ->
        var repos = FlipperResourceLibrary.repositories
        if (type != null) repos = repos.filter { it.resourceType == type }
        if (query.isNotEmpty()) {
            repos = FlipperResourceLibrary.search(query).let { results ->
                if (type != null) results.filter { it.resourceType == type } else results
            }
        }
        repos.sortedByDescending { it.stars }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FlipperResourceLibrary.repositories)

    val resourceTypeCounts: StateFlow<Map<FlipperResourceType, Int>> = flow {
        emit(FlipperResourceType.entries.associateWith { type ->
            FlipperResourceLibrary.getByType(type).size
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ═══════════════════════════════════════════════════════

    init {
        loadInstalledApps()
    }

    // Apps Tab Actions
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun selectCategory(category: FapCategory?) { _selectedCategory.value = category }
    fun selectPack(pack: FapPack?) { _selectedPack.value = pack }
    fun setSortOption(option: SortOption) { _sortBy.value = option }
    fun selectApp(app: FapApp?) { _selectedApp.value = app }
    fun clearError() { _error.value = null }

    // Resources Tab Actions
    fun updateResourceSearch(query: String) { _resourceSearchQuery.value = query }
    fun selectResourceType(type: FlipperResourceType?) { _selectedResourceType.value = type }
    fun selectRepo(repo: FlipperResourceRepo?) { _selectedRepo.value = repo }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rootResult = flipperFileSystem.listDirectory("/ext/apps")
                if (rootResult.isSuccess) {
                    val rootEntries = rootResult.getOrNull() ?: emptyList()
                    val fapEntries = rootEntries.filter { !it.isDirectory && it.name.endsWith(".fap") }
                    val dirEntries = rootEntries.filter { it.isDirectory }
                    val nestedFaps = mutableListOf<String>()
                    for (dir in dirEntries) {
                        val nestedResult = flipperFileSystem.listDirectory(dir.path)
                        if (nestedResult.isSuccess) {
                            nestedFaps.addAll(nestedResult.getOrNull().orEmpty().filter { !it.isDirectory && it.name.endsWith(".fap") }.map { it.name })
                        }
                    }
                    _installedApps.value = (fapEntries.map { it.name } + nestedFaps).map { it.removeSuffix(".fap") }.toSet()
                }
            } catch (_: Exception) { }
            finally { _isLoading.value = false }
        }
    }

    fun installApp(app: FapApp) {
        viewModelScope.launch {
            try {
                installAppNow(app)
                kotlinx.coroutines.delay(2000)
                _installStatus.value = _installStatus.value - app.id
            } catch (e: Exception) {
                _installStatus.value = _installStatus.value + (app.id to InstallStatus.Error(e.message ?: "Unknown error"))
                _error.value = "Failed to install ${app.name}: ${e.message}"
            }
        }
    }

    fun installPack(pack: FapPack?) {
        if (_packProgress.value != null) return
        viewModelScope.launch {
            val apps = if (pack == null) FapHubCatalog.bareApps else FapHubCatalog.appsInPack(pack)
            val failures = mutableListOf<String>()
            apps.forEachIndexed { index, app ->
                _packProgress.value = "Installing ${index + 1}/${apps.size}: ${app.name}"
                try {
                    installAppNow(app)
                    _installStatus.value = _installStatus.value - app.id
                } catch (e: Exception) {
                    failures += app.name
                    _installStatus.value = _installStatus.value + (app.id to InstallStatus.Error(e.message ?: "Unknown error"))
                }
            }
            _packProgress.value = null
            _error.value = if (failures.isEmpty()) {
                null
            } else {
                "Could not install ${failures.joinToString()}. Connect the Flipper and update its firmware, then retry."
            }
        }
    }

    private suspend fun installAppNow(app: FapApp) {
        _installStatus.value = _installStatus.value + (app.id to InstallStatus.Downloading(0.2f))
        val alias = app.catalogAlias.ifBlank { app.id }
        val deviceInfo = try {
            flipperFileSystem.getDeviceInfo().getOrNull()
        } catch (_: Exception) {
            null
        }
        val downloaded = catalogClient.downloadFap(
            alias,
            deviceInfo?.firmwareVersion,
            deviceInfo?.apiVersion,
        )
        if (!FlipperCatalogJson.isFapElf(downloaded.bytes)) {
            throw java.io.IOException("Catalog did not return a FAP for ${app.name}")
        }
        _installStatus.value = _installStatus.value + (app.id to InstallStatus.Downloading(0.85f))
        val targetPath = FapHubCatalog.installPath(app)
        val installDir = targetPath.substringBeforeLast('/')
        flipperFileSystem.createDirectory(installDir)
        _installStatus.value = _installStatus.value + (app.id to InstallStatus.Installing)
        flipperFileSystem.writeFileBytes(targetPath, downloaded.bytes).getOrThrow()
        _installStatus.value = _installStatus.value + (app.id to InstallStatus.Success)
        _installedApps.value = _installedApps.value + app.id + alias
    }

    fun uninstallApp(app: FapApp) {
        viewModelScope.launch {
            try {
                listOf(
                    FapHubCatalog.installPath(app),
                    "/ext/apps/${app.id}.fap",
                    "/ext/apps/${app.category.name.lowercase()}/${app.id}.fap",
                ).forEach {
                    flipperFileSystem.deleteFile(it)
                }
                _installedApps.value = _installedApps.value - app.id - app.catalogAlias
            } catch (e: Exception) {
                _error.value = "Failed to uninstall ${app.name}: ${e.message}"
            }
        }
    }

    fun refresh() { loadInstalledApps() }
}

enum class SortOption(val displayName: String) {
    DOWNLOADS("Most Popular"),
    RATING("Highest Rated"),
    NAME("Alphabetical"),
    UPDATED("Recently Updated"),
    CATEGORY("By Category")
}
