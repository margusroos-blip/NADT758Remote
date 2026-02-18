package com.nadremote.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NadViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val client = NadClient()
    private val discovery = NadDiscovery(app)
    private val blueOs = BlueOsClient()

    // State
    val nadState: StateFlow<NadState> = client.state
    val connectionStatus: StateFlow<ConnectionStatus> = client.connectionStatus
    val error: StateFlow<String?> = client.error
    val devices: StateFlow<List<FoundDevice>> = discovery.devices
    val isScanning: StateFlow<Boolean> = discovery.isScanning
    
    // BluOS
    val nowPlaying: StateFlow<NowPlaying> = blueOs.nowPlaying
    val presets: StateFlow<List<Preset>> = blueOs.presets

    // Saved device
    val savedIp: StateFlow<String> = prefs.savedIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Settings
    val theme: StateFlow<AppTheme> = prefs.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)
    
    val language: StateFlow<AppLanguage> = prefs.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.ENGLISH)
    
    // Favorite sources (max 4)
    val favoriteSources: StateFlow<List<Int>> = prefs.favoriteSources
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // Favorite presets (max 4)
    val favoritePresets: StateFlow<List<Int>> = prefs.favoritePresets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // Auto-reconnect setting
    val autoReconnect: StateFlow<Boolean> = prefs.autoReconnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // Localized strings
    val strings: StateFlow<StringResources> = language
        .map { Strings.get(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EnglishStrings)
    
    // Computed: sources to display (favorites if set, else enabled sources, max 4)
    val displaySources: StateFlow<Map<Int, String>> = combine(
        nadState,
        favoriteSources
    ) { state, favorites ->
        if (favorites.isNotEmpty()) {
            // NÃ¤ita ainult favorite sisendeid
            favorites.mapNotNull { id ->
                state.sources[id]?.let { name -> id to name }
            }.toMap()
        } else {
            // NÃ¤ita kÃµiki aktiivseid sisendeid (max 4)
            state.enabledSources.entries.take(4).associate { it.key to it.value }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Auto-connect on launch
    init {
        viewModelScope.launch {
            val ip = prefs.savedIp.first()
            if (ip.isNotBlank() && DeviceAddressPolicy.isAllowedDeviceAddress(ip)) {
                client.connect(ip)
                blueOs.connect(ip)
            }
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Connection
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun connect(ip: String) {
        if (ip.isBlank() || !DeviceAddressPolicy.isAllowedDeviceAddress(ip)) return
        viewModelScope.launch {
            prefs.saveDevice(ip)
        }
        client.connect(ip)
        blueOs.connect(ip)
    }
    
    /**
     * Try to reconnect when app comes back to foreground.
     * Called from MainActivity onResume.
     */
    fun tryAutoReconnect() {
        viewModelScope.launch {
            val shouldReconnect = prefs.autoReconnect.first()
            val ip = prefs.savedIp.first()
            val status = connectionStatus.value
            
            if (shouldReconnect && ip.isNotBlank() &&
                DeviceAddressPolicy.isAllowedDeviceAddress(ip) &&
                (status == ConnectionStatus.DISCONNECTED || status == ConnectionStatus.ERROR)) {
                client.connect(ip)
                blueOs.connect(ip)
            }
        }
    }

    fun disconnect() {
        client.disconnect()
        blueOs.disconnect()
    }

    fun reconnect() {
        viewModelScope.launch {
            val ip = prefs.savedIp.first()
            if (ip.isNotBlank() && DeviceAddressPolicy.isAllowedDeviceAddress(ip)) {
                client.connect(ip)
                blueOs.connect(ip)
            }
        }
    }

    fun clearDevice() {
        viewModelScope.launch {
            prefs.clear()
            client.disconnect()
            blueOs.disconnect()
            discovery.clear()
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Settings
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            prefs.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(language)
        }
    }
    
    fun setFavoriteSources(sourceIds: List<Int>) {
        viewModelScope.launch {
            prefs.setFavoriteSources(sourceIds)
        }
    }
    
    fun setFavoritePresets(presetIds: List<Int>) {
        viewModelScope.launch {
            prefs.setFavoritePresets(presetIds)
        }
    }
    
    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoReconnect(enabled)
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Controls
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun powerToggle() = client.powerToggle()
    fun powerOn() = client.powerOn()
    fun powerOff() = client.powerOff()

    fun volumeUp() = client.volumeUp()
    fun volumeDown() = client.volumeDown()
    fun setVolume(db: Int) = client.setVolume(db)

    fun muteToggle() = client.muteToggle()

    fun setSource(id: Int) = client.setSource(id)

    fun refresh() {
        client.refresh()
        blueOs.refresh()
    }
    
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // BluOS Controls
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    
    fun playPreset(presetId: Int) = blueOs.playPreset(presetId)
    fun blueOsPlay() = blueOs.play()
    fun blueOsPause() = blueOs.pause()
    fun blueOsNext() = blueOs.next()
    fun blueOsPrevious() = blueOs.previous()
    
    /**
     * Proovib jätkata olemasolevat Spotify sessiooni BluOS kaudu.
     */
    suspend fun tryResumeSpotify(): Boolean = blueOs.tryResumeSpotify()

    /**
     * Käivitab Spotify BluOS seadmes ilma telefoni Spotify äppi avamata.
     */
    suspend fun startSpotifyOnBlueOs(): Boolean = blueOs.startSpotifyOnBlueOs()

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Discovery
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun startMdns() = discovery.startMdns()
    fun stopMdns() = discovery.stopMdns()

    fun scanSubnet() {
        viewModelScope.launch {
            discovery.scanSubnet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
        blueOs.close()
        discovery.close()
    }
}


