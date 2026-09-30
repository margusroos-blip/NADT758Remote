package com.nadremote.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NadViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val client = NadClient()
    private val discovery = NadDiscovery(app)
    private val blueOs = BlueOsClient()
    private var reconnectJob: Job? = null
    @Volatile
    private var inForeground = false

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

    val quickButtonOrder: StateFlow<List<String>> = prefs.quickButtonOrder
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

        // Kui töötav ühendus katkeb (nt Wi-Fi kadus hetkeks), proovi äpi esiplaanil olles
        // uuesti. Ainult üleminek CONNECTED -> ERROR, et ebaõnnestunud katsed ei tekitaks
        // lõputut kordustsüklit.
        viewModelScope.launch {
            var previous = connectionStatus.value
            connectionStatus.collect { status ->
                if (previous == ConnectionStatus.CONNECTED && status == ConnectionStatus.ERROR &&
                    inForeground && reconnectJob?.isActive != true) {
                    startReconnectWindow(force = false)
                }
                previous = status
            }
        }
    }

    /**
     * Lühike reconnect-aknas tehtav retry-loop.
     * See katab juhtumi, kus receiver ärkab standby'st mõned sekundid pärast appi avamist.
     *
     * @param verifyExisting kui olek on CONNECTED, kontrolli enne, kas ühendus päriselt vastab
     * (pärast pikka taustal olekut võib socket olla surnud, kuigi olek pole veel muutunud).
     */
    private fun startReconnectWindow(force: Boolean = false, verifyExisting: Boolean = false) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            val ip = prefs.savedIp.first()
            val allowReconnect = force || prefs.autoReconnect.first()
            if (!allowReconnect || ip.isBlank() || !DeviceAddressPolicy.isAllowedDeviceAddress(ip)) {
                return@launch
            }

            if (verifyExisting && connectionStatus.value == ConnectionStatus.CONNECTED) {
                if (client.verifyAlive()) return@launch
                client.connect(ip, force = true)
            }

            repeat(12) {
                when (connectionStatus.value) {
                    ConnectionStatus.CONNECTED -> return@launch
                    // Katse on pooleli - anna sellele aega, ära katkesta.
                    // OkHttp connect timeout lõpetab rippuva katse ja olek läheb ERROR-iks.
                    ConnectionStatus.CONNECTING -> Unit
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> client.connect(ip)
                }
                if (!blueOs.isPolling.value) {
                    blueOs.connect(ip)
                }
                delay(2500)
            }
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Connection
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun connect(ip: String) {
        if (ip.isBlank() || !DeviceAddressPolicy.isAllowedDeviceAddress(ip)) return
        reconnectJob?.cancel()
        viewModelScope.launch {
            prefs.saveDevice(ip)
        }
        client.connect(ip, force = true)
        blueOs.connect(ip)
    }

    /**
     * Try to reconnect when app comes back to foreground.
     * Called from MainActivity onResume.
     */
    fun tryAutoReconnect() {
        inForeground = true
        startReconnectWindow(force = false, verifyExisting = true)
    }

    /** Called from MainActivity onPause. */
    fun onAppPaused() {
        inForeground = false
    }

    /** Loeb salvestatud IP otse DataStore'ist (savedIp StateFlow algväärtus on "" enne laadimist). */
    suspend fun hasSavedDevice(): Boolean = prefs.savedIp.first().isNotBlank()

    fun disconnect() {
        reconnectJob?.cancel()
        client.disconnect()
        blueOs.disconnect()
    }

    fun reconnect() {
        reconnectJob?.cancel()
        viewModelScope.launch {
            val ip = prefs.savedIp.first()
            if (ip.isNotBlank() && DeviceAddressPolicy.isAllowedDeviceAddress(ip)) {
                client.connect(ip, force = true)
                blueOs.connect(ip)
            }
        }
    }

    fun clearDevice() {
        reconnectJob?.cancel()
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

    fun setQuickButtonOrder(order: List<String>) {
        viewModelScope.launch {
            prefs.setQuickButtonOrder(order)
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
    
    fun playPreset(presetId: Int) {
        blueOs.playPreset(presetId)
        startReconnectWindow(force = true)
    }
    fun blueOsPlay() = blueOs.play()
    fun blueOsPause() = blueOs.pause()
    fun blueOsNext() = blueOs.next()
    fun blueOsPrevious() = blueOs.previous()
    
    /**
     * Proovib jätkata olemasolevat Spotify sessiooni BluOS kaudu.
     */
    suspend fun tryResumeSpotify(): Boolean = withContext(Dispatchers.IO) {
        blueOs.tryResumeSpotify()
    }

    /**
     * Käivitab Spotify BluOS seadmes ilma telefoni Spotify äppi avamata.
     */
    suspend fun startSpotifyOnBlueOs(): Boolean = withContext(Dispatchers.IO) {
        blueOs.startSpotifyOnBlueOs()
    }

    suspend fun saveCurrentBluOsPreset(name: String): Boolean = withContext(Dispatchers.IO) {
        blueOs.saveCurrentAsPreset(name)
    }

    suspend fun browseTuneIn(key: String?): List<BrowseEntry> = withContext(Dispatchers.IO) {
        blueOs.browseTuneIn(key)
    }

    suspend fun playBrowseEntry(entry: BrowseEntry): Boolean = withContext(Dispatchers.IO) {
        val played = blueOs.playBrowseEntry(entry)
        if (played) {
            startReconnectWindow(force = true)
        }
        played
    }

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
        reconnectJob?.cancel()
        client.disconnect()
        blueOs.close()
        discovery.close()
    }
}


