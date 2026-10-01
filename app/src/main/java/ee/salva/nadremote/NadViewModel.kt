package com.nadremote.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class NadViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val client = NadClient()
    private val discovery = NadDiscovery(app)
    private val blueOs = BlueOsClient()
    private var reconnectJob: Job? = null
    private val searchMutex = Mutex()

    // "Ühendamine…" olek (vt startPending) — siin üleval, sest activeSourceId kasutab seda
    private val _pendingPlay = MutableStateFlow<PendingPlay?>(null)
    val pendingPlay: StateFlow<PendingPlay?> = _pendingPlay.asStateFlow()

    // Kasutaja valitud virtuaalne sisend (Raadio/Spotify), et õige player ilmuks kohe
    private val _virtualIntent = MutableStateFlow<Int?>(null)
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

    /**
     * Spotify on NAD-is valitud, aga BluOS on üle 10 s "connecting" olekus: juhtimine töötab,
     * heli voog mitte (Spotify Connecti seanss on katki). Parandab ainult Spotify äpis
     * seadme vahetus (telefon -> NAD), seega näitame kasutajale vihjet.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val spotifyStuck: StateFlow<Boolean> = blueOs.nowPlaying
        .map { it.isSpotify && it.state.equals("connecting", ignoreCase = true) }
        .distinctUntilChanged()
        .flatMapLatest { connecting ->
            if (connecting) flow { emit(false); delay(SPOTIFY_STUCK_MS); emit(true) } else flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Saved device
    val savedIp: StateFlow<String> = prefs.savedIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // NAD-i nimi BluOS-ist, nt "Elutoa ressiiver NAD T758"
    val savedName: StateFlow<String> = prefs.savedName
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
        favoriteSources,
        strings
    ) { state, favorites, s ->
        if (favorites.isNotEmpty()) {
            // NÃ¤ita ainult favorite sisendeid (+ virtuaalsed Raadio/Spotify)
            favorites.mapNotNull { id ->
                when (id) {
                    VirtualSource.RADIO -> id to s.radio
                    VirtualSource.SPOTIFY -> id to "Spotify"
                    else -> state.sources[id]?.let { name -> id to name }
                }
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

        // "Raadio" sisendi jaoks: pea meeles viimati mänginud raadiojaam, ka siis, kui see
        // pandi mängima mujalt (BluOS-i äpp, NAD-i pult). Capture = füüsiline sisend, mitte raadio.
        viewModelScope.launch {
            nowPlaying
                .filter {
                    it.isPlaying && it.isRadio && !it.isSpotify && it.streamUrl.isNotBlank() &&
                        !it.streamUrl.startsWith("Capture:", ignoreCase = true)
                }
                .distinctUntilChangedBy { it.streamUrl }
                .collect { np ->
                    val current = prefs.lastRadio.first()
                    if (current != null && sameStation(current, np.streamUrl)) return@collect
                    rememberRadio(LastRadio(np.stationName.ifBlank { np.displayTitle }, np.streamUrl, np.imageUrl))
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
                if (status == ConnectionStatus.CONNECTED && previous != ConnectionStatus.CONNECTED) {
                    rememberDeviceIdentity()
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
            val startIp = prefs.savedIp.first()
            val allowReconnect = force || prefs.autoReconnect.first()
            if (!allowReconnect || startIp.isBlank() || !DeviceAddressPolicy.isAllowedDeviceAddress(startIp)) {
                return@launch
            }

            if (verifyExisting && connectionStatus.value == ConnectionStatus.CONNECTED) {
                if (client.verifyAlive()) return@launch
                client.connect(startIp, force = true)
            }

            var ip = startIp
            repeat(12) { attempt ->
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

                // ~5 s ilma ühenduseta: võib-olla sai NAD uue IP (nt pärast Wi-Fi pulga
                // taasühendamist). Otsi sama seade võrgust üles.
                if (attempt == 1 && connectionStatus.value != ConnectionStatus.CONNECTED) {
                    relocateDevice(ip)?.let { ip = it }
                }
            }
        }
    }


    /** Otsib võrgust NAD-e (skaneerimine + juba mDNS-iga leitud). */
    private suspend fun searchDevices(): List<FoundDevice> = searchMutex.withLock {
        discovery.scanSubnet()
        discovery.devices.value
    }

    /**
     * Salvestatud IP ei vasta: otsi sama NAD üles MAC-aadressi järgi. Kui MAC-i pole veel
     * salvestatud (vanem install) ja võrgus on täpselt üks NAD, võta see.
     * Tagastab uue IP või null, kui midagi ei muutunud.
     */
    private suspend fun relocateDevice(currentIp: String): String? {
        val savedMac = prefs.savedMac.first()
        val found = searchDevices()
        val match = if (savedMac.isNotBlank()) {
            found.firstOrNull { it.mac.equals(savedMac, ignoreCase = true) }
        } else {
            found.singleOrNull()
        } ?: return null
        if (match.ip == currentIp) return null
        connectTo(match)
        return match.ip
    }

    /**
     * Pärast õnnestunud ühendust salvesta NAD-i nimi ja MAC (BluOS-ist), et seade
     * ära tunda ka siis, kui ruuter annab talle hiljem uue IP.
     */
    private fun rememberDeviceIdentity() {
        viewModelScope.launch {
            val ip = prefs.savedIp.first()
            if (ip.isBlank()) return@launch
            val identity = withContext(Dispatchers.IO) { discovery.identify(ip) }
            if (identity.mac.isBlank()) return@launch
            if (identity.mac != prefs.savedMac.first() || identity.name != prefs.savedName.first()) {
                prefs.saveDevice(ip, identity.name, identity.mac)
            }
        }
    }

    /**
     * Seadete "Otsi NAD": otsib võrgust ja kui ühendust pole, ühendub ise, kui leiab
     * salvestatud seadme (MAC järgi) või kui võrgus on täpselt üks NAD.
     */
    fun findDevices() {
        viewModelScope.launch {
            val found = searchDevices()
            if (connectionStatus.value == ConnectionStatus.CONNECTED) return@launch
            val savedMac = prefs.savedMac.first()
            val pick = found.firstOrNull { savedMac.isNotBlank() && it.mac.equals(savedMac, ignoreCase = true) }
                ?: found.singleOrNull()
            pick?.let {
                reconnectJob?.cancel()
                connectTo(it)
            }
        }
    }

    /** Kasutaja valis leitud seadme nimekirjast. */
    fun connectToDevice(device: FoundDevice) {
        if (!DeviceAddressPolicy.isAllowedDeviceAddress(device.ip)) return
        reconnectJob?.cancel()
        connectTo(device)
    }

    private fun connectTo(device: FoundDevice) {
        viewModelScope.launch {
            prefs.saveDevice(device.ip, device.name, device.mac)
        }
        client.connect(device.ip, force = true)
        blueOs.connect(device.ip)
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

    fun setSource(id: Int) {
        _virtualIntent.value = null
        client.setSource(id)
    }

    // ── Virtuaalsed sisendid (Raadio, Spotify) ─────────────────────────────
    // NAD-i jaoks mõlemad BluOS sisend; äpis eraldi nupud nagu vana ressiiveri "Tuner".

    /** NAD-i BluOS sisendi number (nime järgi, nt "BluOS" = 6). */
    private fun bluOsSourceId(): Int? = nadState.value.sources.entries.firstOrNull { (_, name) ->
        val n = name.lowercase()
        n.contains("bluos") || n.contains("bluesound") || n.contains("stream")
    }?.key

    /** Milline sisend on päriselt aktiivne: BluOS-is mängiv Spotify/raadio = virtuaalne sisend. */
    val activeSourceId: StateFlow<Int> = combine(
        nadState, nowPlaying, favoriteSources, _virtualIntent, _pendingPlay
    ) { state, np, favorites, intent, pending ->
        val onBluOs = bluOsSourceId()?.let { it == state.sourceId } ?: false
        val intentShown = intent?.takeIf { it in favorites }
        when {
            // Just vajutatud (NAD vahetab sisendit / jaam käivitub) — näita kohe valitud sisendit
            intentShown != null && pending != null -> intentShown
            onBluOs && np.isSpotify && VirtualSource.SPOTIFY in favorites -> VirtualSource.SPOTIFY
            onBluOs && np.isRadio && !np.isSpotify && VirtualSource.RADIO in favorites &&
                !np.streamUrl.startsWith("Capture:", ignoreCase = true) -> VirtualSource.RADIO
            // Valitud, aga veel ei mängi midagi (nt raadio leht on lahti)
            intentShown != null && onBluOs && !np.isPlaying -> intentShown
            else -> state.sourceId
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun selectSpotifyInput() {
        _virtualIntent.value = VirtualSource.SPOTIFY
        switchToBluOsInput()
    }

    fun switchToBluOsInput() {
        val id = bluOsSourceId() ?: return
        if (nadState.value.sourceId != id) client.setSource(id)
    }

    // Raadio sisend, aga jaama pole veel kuulatud (või ei õnnestunud) -> ava raadio leht
    private val _radioSheetRequested = MutableStateFlow(false)
    val radioSheetRequested: StateFlow<Boolean> = _radioSheetRequested.asStateFlow()
    fun onRadioSheetShown() { _radioSheetRequested.value = false }

    /** "Raadio" sisend: BluOS sisend + viimati kuulatud jaam (nagu vana tuuner). */
    fun selectRadioInput() {
        _virtualIntent.value = VirtualSource.RADIO
        switchToBluOsInput()
        viewModelScope.launch {
            val last = prefs.lastRadio.first()
            if (last == null) {
                _radioSheetRequested.value = true
                return@launch
            }
            // Juba mängib see jaam — ära alusta uuesti
            val np = nowPlaying.value
            if (np.isPlaying && np.isRadio && !np.isSpotify && sameStation(last, np.streamUrl)) return@launch
            startPending(last.name, last.imageUrl)
            val ok = withContext(Dispatchers.IO) { blueOs.playRadio(last) }
            if (ok) {
                startReconnectWindow(force = true)
            } else {
                clearPending()
                _radioSheetRequested.value = true
            }
        }
    }

    private fun sameStation(radio: LastRadio, streamUrl: String): Boolean {
        if (streamUrl.isBlank()) return false
        val urls = listOfNotNull(radio.url.takeIf { it.isNotBlank() }, urlParamOf(radio.playUrl))
        return urls.any { u -> streamUrl == u || streamUrl.startsWith("$u/") || u.startsWith("$streamUrl/") }
    }

    /** BluOS-i /Play?url=... -> url parameeter (nt "TuneIn:s25067"). */
    private fun urlParamOf(playUrl: String): String? {
        if (playUrl.isBlank()) return null
        val full = if (playUrl.startsWith("http", ignoreCase = true)) playUrl else "http://nad$playUrl"
        return full.toHttpUrlOrNull()?.queryParameter("url")?.takeIf { it.isNotBlank() }
    }

    private fun rememberRadio(radio: LastRadio) {
        if (radio.url.startsWith("Spotify:", ignoreCase = true)) return
        viewModelScope.launch { prefs.setLastRadio(radio) }
    }

    fun refresh() {
        client.refresh()
        blueOs.refresh()
    }
    
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // BluOS Controls
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    
    fun playPreset(presetId: Int) {
        val preset = presets.value.firstOrNull { it.id == presetId }
        startPending(preset?.name.orEmpty(), preset?.imageUrl.orEmpty())
        preset?.let { rememberRadio(LastRadio(it.name, it.url, it.imageUrl)) }
        blueOs.playPreset(presetId)
        startReconnectWindow(force = true)
    }

    // ── "Ühendamine…" olek ──────────────────────────────────────────────────
    // Jaama vahetus (eriti Spotify -> TuneIn) võtab BluOS-il mitu sekundit. Selle aja
    // näitab mini-player kohe valitud jaama ja laadimisringi, et äpp ei tunduks tardunud.

    private var pendingJob: Job? = null

    private fun startPending(title: String, imageUrl: String) {
        val before = nowPlaying.value
        _pendingPlay.value = PendingPlay(title, imageUrl)
        pendingJob?.cancel()
        pendingJob = viewModelScope.launch {
            withTimeoutOrNull(PENDING_TIMEOUT_MS) {
                nowPlaying.first { np ->
                    np.isPlaying && np.hasContent && (
                        !before.isPlaying ||
                            np.service != before.service ||
                            np.displayTitle != before.displayTitle ||
                            np.imageUrl != before.imageUrl ||
                            (title.isNotBlank() && np.stationName.contains(title, ignoreCase = true))
                        )
                }
            }
            _pendingPlay.value = null
        }
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
    suspend fun startSpotifyOnBlueOs(): Boolean {
        if (!nowPlaying.value.isSpotify || !nowPlaying.value.isPlaying) {
            startPending("Spotify", "")
        }
        val started = withContext(Dispatchers.IO) { blueOs.startSpotifyOnBlueOs() }
        if (!started) clearPending()
        return started
    }

    private fun clearPending() {
        pendingJob?.cancel()
        _pendingPlay.value = null
    }

    // ── Lemmikute haldus (muudab lemmikuid NAD-is endas) ─────────────────────

    suspend fun addCurrentAsPreset(): Boolean = withContext(Dispatchers.IO) {
        blueOs.addCurrentAsPreset()
    }

    suspend fun addStationPreset(entry: BrowseEntry): Boolean = withContext(Dispatchers.IO) {
        blueOs.addStationPreset(entry)
    }

    suspend fun renamePreset(preset: Preset, newName: String): Boolean = withContext(Dispatchers.IO) {
        blueOs.renamePreset(preset, newName)
    }

    suspend fun deletePreset(preset: Preset): Boolean = withContext(Dispatchers.IO) {
        blueOs.deletePreset(preset)
    }

    suspend fun browseTuneIn(key: String?): List<BrowseEntry> = withContext(Dispatchers.IO) {
        blueOs.browseTuneIn(key)
    }

    // Raadio: otsib TuneIn-ist ja Airable-ist korraga, iga jaam üks kord
    suspend fun searchRadio(query: String): List<BrowseEntry> = withContext(Dispatchers.IO) {
        blueOs.searchRadio(query)
    }

    suspend fun localRadio(): List<BrowseEntry> = withContext(Dispatchers.IO) {
        blueOs.localRadio()
    }

    suspend fun tuneInQuality(stationId: String): StreamQuality? = withContext(Dispatchers.IO) {
        blueOs.tuneInQuality(stationId)
    }

    suspend fun playBrowseEntry(entry: BrowseEntry): Boolean {
        withContext(Dispatchers.Main) { startPending(entry.title, "") }
        val played = withContext(Dispatchers.IO) { blueOs.playBrowseEntry(entry) }
        withContext(Dispatchers.Main) {
            if (played) {
                startReconnectWindow(force = true)
                urlParamOf(entry.playUrl)?.let { url ->
                    rememberRadio(LastRadio(entry.title, url, entry.imageUrl, entry.playUrl))
                }
            } else {
                clearPending()
            }
        }
        return played
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Discovery
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun startMdns() = discovery.startMdns()
    fun stopMdns() = discovery.stopMdns()


    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        client.disconnect()
        blueOs.close()
        discovery.close()
    }
}



/** Valitud, aga veel mängima hakkamata jaam/allikas (mini-player näitab "Ühendamine…"). */
data class PendingPlay(
    val title: String,
    val imageUrl: String
)

private const val PENDING_TIMEOUT_MS = 20_000L
private const val SPOTIFY_STUCK_MS = 10_000L
