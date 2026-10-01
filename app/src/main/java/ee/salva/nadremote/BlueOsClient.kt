package com.nadremote.app

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * BluOS HTTP API Client
 * 
 * BluOS (NAD T758 v3 streaming moodul) kasutab HTTP API-t pordil 11000.
 * PÃµhilised endpoint-id:
 *   - /Status - praegune taasesituse staatus (toetab long-polling)
 *   - /Presets - salvestatud presetid (TuneIn raadiod jm)
 *   - /Browse - sirvimiseks (Spotify, TuneIn, Local jne)
 * 
 * Long-polling: /Status?timeout=100&etag=xxx
 * Tagastab vastuse ainult siis kui staatus muutub, vÃµi timeout'i jÃ¤rel.
 * See vÃ¤hendab oluliselt pÃ¤ringute arvu ja annab reaalajas uuendusi
 * (nt kui Spotify Connect hakkab mÃ¤ngima, saame sellest kohe teada).
 */
class BlueOsClient {

    companion object {
        private const val TAG = "BlueOS"
        private const val LONG_POLL_TIMEOUT = 100 // sekundit (BluOS soovitab 100)
        private const val SPOTIFY_WAIT_ATTEMPTS = 12
        private const val SPOTIFY_WAIT_STEP_MS = 350L
        private val TUNEIN_STATION = Regex("^TuneIn:s\\d+$", RegexOption.IGNORE_CASE)
    }

    // Tavaline klient kiirte pÃ¤ringute jaoks (presets, commands)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Long-polling klient - pikem timeout
    private val longPollClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(LONG_POLL_TIMEOUT.toLong() + 15, TimeUnit.SECONDS)
        .build()

    private var ip: String = ""
    private var pollingJob: Job? = null
    private var currentEtag: String = ""
    private var cachedSpotifySourceUrl: String? = null
    private var cachedSpotifyPresetId: Int? = null
    private var statusActions: List<StatusAction> = emptyList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()
    
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()
    
    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Connection
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun connect(deviceIp: String) {
        if (deviceIp.isBlank() || !DeviceAddressPolicy.isAllowedDeviceAddress(deviceIp)) return
        ip = deviceIp
        currentEtag = ""
        startPolling()
    }

    fun disconnect() {
        stopPolling()
        ip = ""
        currentEtag = ""
        cachedSpotifySourceUrl = null
        cachedSpotifyPresetId = null
        cachedLocalRadio = null
        cachedAirableLocal = null
        statusActions = emptyList()
        _nowPlaying.value = NowPlaying()
        _presets.value = emptyList()
    }

    private fun startPolling() {
        stopPolling()
        _isPolling.value = true
        
        pollingJob = scope.launch {
            // Esmalt lae presetid
            fetchPresets()
            
            // Esmane status pÃ¤ring (ilma long-polling'uta)
            fetchStatus(longPoll = false)
            
            // Siis alusta long-polling loop
            while (isActive) {
                try {
                    fetchStatus(longPoll = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Long-poll error: ${e.message}")
                    // Vea korral oota ja proovi uuesti
                    delay(3000)
                    // Reset etag et saada uus algseis
                    currentEtag = ""
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
        currentEtag = ""
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Status - Now Playing (long-polling tugi)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private suspend fun fetchStatus(longPoll: Boolean = false) {
        if (ip.isBlank()) return
        
        try {
            val url = if (longPoll && currentEtag.isNotBlank()) {
                "http://$ip:11000/Status?timeout=$LONG_POLL_TIMEOUT&etag=$currentEtag"
            } else {
                "http://$ip:11000/Status"
            }

            val httpClient = if (longPoll) longPollClient else client

            val request = Request.Builder()
                .url(url)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { xml ->
                        parseStatus(xml)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (longPoll) throw e // Las polling loop kÃ¤sitleb
            // Esmase pÃ¤ringu viga - ignoreeri vaikselt
        }
    }

    private fun parseStatus(xml: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var etag = ""
            var state = ""
            var streamFormat = ""
            var title1 = ""
            var title2 = ""
            var title3 = ""
            var artist = ""
            var album = ""
            var name = ""
            var service = ""
            var serviceIcon = ""
            var image = ""
            var streamUrl: String? = null  // null = puudub, "" = olemas aga tÃ¼hi
            var canSeek = false
            var totlen = 0
            var secs = 0
            var currentTag = ""
            val actions = mutableListOf<StatusAction>()
            
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "status") {
                            etag = parser.getAttributeValue(null, "etag") ?: ""
                        }
                        if (currentTag == "action") {
                            val actionUrl = parser.getAttributeValue(null, "url").orEmpty().trim()
                            if (actionUrl.isNotBlank()) {
                                actions.add(
                                    StatusAction(
                                        name = parser.getAttributeValue(null, "name").orEmpty(),
                                        text = parser.getAttributeValue(null, "text").orEmpty(),
                                        url = actionUrl
                                    )
                                )
                            }
                        }
                        // streamUrl olemasolu on oluline isegi kui sisu on tÃ¼hi
                        if (currentTag == "streamUrl") {
                            streamUrl = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotBlank()) {
                            when (currentTag) {
                                "state" -> state = text
                                "streamFormat" -> streamFormat = text
                                "title1" -> title1 = text
                                "title2" -> title2 = text
                                "title3" -> title3 = text
                                "artist" -> artist = text
                                "album" -> album = text
                                "name" -> name = text
                                "service" -> service = text
                                "serviceIcon" -> serviceIcon = text
                                "image" -> image = text
                                "streamUrl" -> streamUrl = text
                                "canSeek" -> canSeek = text == "1"
                                "totlen" -> totlen = text.toIntOrNull() ?: 0
                                "secs" -> secs = text.toIntOrNull() ?: 0
                            }
                        }
                    }
                }
                parser.next()
            }
            
            // Salvesta etag long-polling jaoks
            if (etag.isNotBlank()) {
                currentEtag = etag
            }
            
            // BluOS API spec:
            // - <streamUrl> olemaolu = play queue EI ole allikas
            //   (raadio, external stream - shuffle/repeat/next/prev pole saadaval)
            // - <streamUrl> puudub = play queue on allikas (Spotify, local, Deezer...)
            //   (canSeek, next, prev on saadaval)
            val hasStreamUrl = streamUrl != null
            
            val nowPlaying = NowPlaying(
                isPlaying = state == "stream" || state == "play",
                state = state,
                streamFormat = streamFormat,
                streamUrl = streamUrl.orEmpty(),
                service = service,
                serviceIcon = serviceIcon,
                artist = artist.ifBlank { title2 },
                track = title1,
                album = album.ifBlank { title3 },
                stationName = name,
                showName = if (hasStreamUrl || service.contains("TuneIn", true)) {
                    title1.ifBlank { title2 }
                } else "",
                imageUrl = when {
                    image.startsWith("http") -> image
                    image.isNotBlank() -> "http://$ip:11000$image?followRedirects=1"
                    else -> ""
                },
                canSkip = !hasStreamUrl,  // Skip/Back ainult kui play queue on allikas
                canSeek = canSeek,
                totalSeconds = totlen,
                currentSeconds = secs,
                isStream = hasStreamUrl,
                canSavePreset = actions.any { isSavePresetAction(it) }
            )
            
            statusActions = actions
            _nowPlaying.value = nowPlaying
            
        } catch (e: Exception) {
            Log.w(TAG, "Status parse error: ${e.message}")
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Presets
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    suspend fun fetchPresets() {
        if (ip.isBlank()) return
        
        try {
            val request = Request.Builder()
                .url("http://$ip:11000/Presets")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { xml ->
                        parsePresets(xml)
                        Log.d(TAG, "Parsed presets: ${_presets.value.size}")
                    }
                } else {
                    Log.e(TAG, "Presets request failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Presets error: ${e.message}")
        }
    }

    private fun parsePresets(xml: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            val presetList = mutableListOf<Preset>()
            var currentPreset: Preset? = null
            var currentTag = ""
            var inPreset = false
            
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "preset") {
                            inPreset = true
                            val id = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            val presetName = parser.getAttributeValue(null, "name") ?: ""
                            val presetImage = parser.getAttributeValue(null, "image") ?: ""
                            currentPreset = Preset(id = id, url = url, name = presetName, imageUrl = presetImage)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inPreset) {
                            val text = parser.text?.trim() ?: ""
                            if (text.isNotBlank()) {
                                currentPreset?.let { preset ->
                                    when (currentTag) {
                                        "name" -> if (preset.name.isBlank()) currentPreset = preset.copy(name = text)
                                        "image" -> if (preset.imageUrl.isBlank()) currentPreset = preset.copy(imageUrl = text)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "preset") {
                            inPreset = false
                            currentPreset?.let { 
                                if (it.name.isNotBlank() || it.id > 0) {
                                    val fixedPreset = if (it.imageUrl.isNotBlank() && !it.imageUrl.startsWith("http")) {
                                        it.copy(imageUrl = "http://$ip:11000${it.imageUrl}?followRedirects=1")
                                    } else it
                                    presetList.add(fixedPreset)
                                }
                            }
                            currentPreset = null
                        }
                    }
                }
                parser.next()
            }
            
            _presets.value = presetList.sortedBy { it.id }
            
        } catch (e: Exception) {
            Log.w(TAG, "Presets parse error: ${e.message}")
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Controls
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun playPreset(presetId: Int) {
        if (ip.isBlank()) return
        
        scope.launch {
            playPresetInternal(presetId)
        }
    }

    fun play() = sendCommand("Play")
    fun pause() = sendCommand("Pause?toggle=1")
    fun stop() = sendCommand("Stop")
    fun next() = sendCommand("Skip")
    fun previous() = sendCommand("Back")

    /**
     * TuneIn-i otsing nime järgi (/Browse?key=TuneIn:Search&q=...).
     * Tagastab ainult mängitavad jaamad; saated/podcastid jäetakse välja.
     */
    suspend fun searchTuneIn(query: String): List<BrowseEntry> {
        if (ip.isBlank() || query.isBlank()) return emptyList()
        val url = "http://$ip:11000/Browse".toHttpUrl().newBuilder()
            .addQueryParameter("key", "TuneIn:Search")
            .addQueryParameter("q", query.trim())
            .build()
            .toString()
        // Jaamad on "TuneIn:s123", saadete episoodid "TuneIn:t123" — näitame ainult jaamu.
        // Sama jaam võib olla nii "Top Results" kui "Stations" all, seega distinct.
        return fetchBrowseItems(url)
            .mapNotNull { item ->
                val play = item.playUrl.ifBlank { item.autoplayUrl }
                val stationUrl = normalizeApiUrl(play).toHttpUrlOrNull()?.queryParameter("url")
                if (stationUrl != null && TUNEIN_STATION.matches(stationUrl)) stationUrl to item else null
            }
            .distinctBy { it.first }
            .map { it.second.toBrowseEntry() }
    }

    // Jaama voo kvaliteet TuneIn-ist; UNKNOWN_QUALITY = küsitud, aga teadmata
    private val qualityCache = java.util.concurrent.ConcurrentHashMap<String, StreamQuality>()

    /**
     * Jaama voo formaat ja bitrate TuneIn-i avalikust API-st (BluOS seda ei anna).
     * Väljaminev päring sisaldab ainult jaama ID-d (nt "s25067"), mitte kasutaja andmeid.
     */
    fun tuneInQuality(stationId: String): StreamQuality? {
        if (stationId.isBlank()) return null
        qualityCache[stationId]?.let { return it.takeIf { q -> q != UNKNOWN_QUALITY } }
        val quality = try {
            val url = "https://opml.radiotime.com/Tune.ashx".toHttpUrl().newBuilder()
                .addQueryParameter("id", stationId)
                .addQueryParameter("render", "json")
                .addQueryParameter("formats", "mp3,aac,ogg,hls")
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = org.json.JSONObject(response.body?.string().orEmpty()).optJSONArray("body")
                    ?: return@use null
                (0 until body.length())
                    .mapNotNull { body.optJSONObject(it) }
                    .firstOrNull { it.optString("element") == "audio" && it.optInt("bitrate") > 0 }
                    ?.let { StreamQuality(it.optString("media_type").uppercase(), it.optInt("bitrate")) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TuneIn quality failed ($stationId): ${e.message}")
            null
        }
        qualityCache[stationId] = quality ?: UNKNOWN_QUALITY
        return quality
    }

    private var cachedLocalRadio: List<BrowseEntry>? = null

    /**
     * TuneIn-i "Local Radio": NAD-i asukoha järgi kohalikud jaamad (Eestis nt Raadio 2,
     * Kuku, Power Hit). Tulemus hoitakse seansi ajaks meeles, et leht avaneks kohe.
     */
    suspend fun tuneInLocalRadio(): List<BrowseEntry> {
        if (ip.isBlank()) return emptyList()
        cachedLocalRadio?.let { if (it.isNotEmpty()) return it }
        val root = fetchBrowseItems(buildBrowseByKeyUrl("TuneIn:"))
        val local = root.firstOrNull { it.browseKey.contains("categories%252Flocal", ignoreCase = true) }
            ?: root.firstOrNull { it.text.equals("Local Radio", ignoreCase = true) }
            ?: return emptyList()
        val stations = fetchBrowseItems(buildBrowseByKeyUrl(local.browseKey))
            .filter { it.playUrl.isNotBlank() || it.autoplayUrl.isNotBlank() }
            .map { it.toBrowseEntry() }
        cachedLocalRadio = stations
        return stations
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Raadio = TuneIn + Airable (BluOS-i "Radio" menüü). Kasutaja ei pea teadma,
    // kummast kataloogist jaam tuleb: otsime mõlemast ja näitame iga jaama üks kord.
    // Kui jaam on mõlemas, eelistame TuneIn-i (selle kvaliteeti näeme ette).
    // ═══════════════════════════════════════════════════════════════════════

    /** Airable'i otsing: BluOS annab esmalt lingid ("Stations", "Podcasts"), võtame jaamad. */
    suspend fun searchAirable(query: String): List<BrowseEntry> {
        if (ip.isBlank() || query.isBlank()) return emptyList()
        val url = "http://$ip:11000/Browse".toHttpUrl().newBuilder()
            .addQueryParameter("key", "Airable:Search")
            .addQueryParameter("q", query.trim())
            .build()
            .toString()
        val stationsLink = fetchBrowseItems(url)
            .firstOrNull { it.browseKey.contains("c=station", ignoreCase = true) }
            ?: return emptyList()
        return fetchBrowseItems(buildBrowseByKeyUrl(stationsLink.browseKey))
            .filter { it.playUrl.isNotBlank() || it.autoplayUrl.isNotBlank() }
            .map { it.toBrowseEntry() }
    }

    private var cachedAirableLocal: List<BrowseEntry>? = null

    /** Airable'i "Local stations" (NAD-i asukoha järgi). */
    suspend fun airableLocalRadio(): List<BrowseEntry> {
        if (ip.isBlank()) return emptyList()
        cachedAirableLocal?.let { if (it.isNotEmpty()) return it }
        val local = fetchBrowseItems(buildBrowseByKeyUrl("Airable:"))
            .firstOrNull { it.text.equals("Local stations", ignoreCase = true) }
            ?: return emptyList()
        val stations = fetchBrowseItems(buildBrowseByKeyUrl(local.browseKey))
            .filter { it.playUrl.isNotBlank() || it.autoplayUrl.isNotBlank() }
            .map { it.toBrowseEntry() }
        cachedAirableLocal = stations
        return stations
    }

    suspend fun searchRadio(query: String): List<BrowseEntry> = coroutineScope {
        val tuneIn = async { searchTuneIn(query) }
        val airable = async { searchAirable(query) }
        mergeStations(tuneIn.await(), airable.await())
    }

    suspend fun localRadio(): List<BrowseEntry> = coroutineScope {
        val tuneIn = async { tuneInLocalRadio() }
        val airable = async { airableLocalRadio() }
        mergeStations(tuneIn.await(), airable.await())
    }

    /**
     * Üks nimekiri: TuneIn-i järjekord, samanimeline Airable'i jaam kohe tema järel
     * (UI näitab mõlemal kataloogi silti, kasutaja valib ise). Ülejäänud Airable'i jaamad lõppu.
     */
    private fun mergeStations(primary: List<BrowseEntry>, secondary: List<BrowseEntry>): List<BrowseEntry> {
        val remaining = secondary.toMutableList()
        val result = mutableListOf<BrowseEntry>()
        for (entry in primary) {
            result += entry
            val key = radioNameKey(entry.title)
            if (key.isBlank()) continue
            val twins = remaining.filter { radioNameKey(it.title) == key }
            result += twins
            remaining -= twins.toSet()
        }
        return result + remaining
    }

    suspend fun browseTuneIn(key: String?): List<BrowseEntry> {
        if (ip.isBlank()) return emptyList()

        return if (key.isNullOrBlank()) {
            val rootItems = fetchBrowseItems("http://$ip:11000/Browse")
            val tuneInRoot = rootItems.firstOrNull { isTuneInBrowseItem(it) } ?: return emptyList()
            val browseKey = tuneInRoot.browseKey
            val tuneInItems = if (browseKey.isNotBlank()) {
                fetchBrowseItems(buildBrowseByKeyUrl(browseKey))
            } else {
                listOf(tuneInRoot)
            }
            tuneInItems.map { it.toBrowseEntry() }
        } else {
            fetchBrowseItems(buildBrowseByKeyUrl(key)).map { it.toBrowseEntry() }
        }
    }

    suspend fun playBrowseEntry(entry: BrowseEntry): Boolean {
        if (ip.isBlank()) return false
        val candidates = listOf(entry.autoplayUrl, entry.playUrl, entry.actionUrl)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (candidate in candidates) {
            if (playBrowseUrl(candidate)) return true
        }
        return false
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lemmikute (BluOS presetid) haldus
    //   lisa:            /SetPreset?name=&url=&image=&service=   (saab järgmise vaba id)
    //   nimeta ümber:    /SetPreset?id=N&name=&url=&image=&service=  (id ja url jäävad)
    //   kustuta:         /SetPreset?id=N&delete=1
    //   praegu mängiv:   Status <action type="preset"> URL (nt /Action?action=addPreset&service=Spotify)
    // Kontrollitud NAD T758 / BluOS 4.14.12 peal (2026-09-30). NB: muudab lemmikuid
    // NAD-is endas — need on nähtavad ka BluOS-i äpis.
    // ═══════════════════════════════════════════════════════════════════════

    /** Lisab praegu mängiva (nt Spotify playlist/album) lemmikuks, kui BluOS seda pakub. */
    suspend fun addCurrentAsPreset(): Boolean {
        if (ip.isBlank()) return false
        fetchStatus(longPoll = false)
        val action = statusActions.firstOrNull { isSavePresetAction(it) } ?: return false
        val before = _presets.value.size
        val ok = httpGetOk(normalizeApiUrl(action.url))
        if (ok) refreshPresetsAfterChange()
        return ok && _presets.value.size > before
    }

    /** Lisab TuneIn-i otsingust leitud jaama lemmikuks. */
    suspend fun addStationPreset(entry: BrowseEntry): Boolean {
        val play = entry.playUrl.ifBlank { entry.autoplayUrl }
        val parsed = normalizeApiUrl(play).toHttpUrlOrNull() ?: return false
        val url = parsed.queryParameter("url")?.takeIf { it.isNotBlank() } ?: return false
        val image = parsed.queryParameter("image").orEmpty()
        return setPreset(id = null, name = entry.title, url = url, image = image)
    }

    suspend fun renamePreset(preset: Preset, newName: String): Boolean {
        // fetchPresets teeb suhtelisest pildist täis-URL-i (NAD-i IP-ga); saada tagasi
        // algne suhteline kuju, muidu jääks lemmikusse vana IP ja pilt läheks IP muutudes katki
        val localPrefix = "http://$ip:11000"
        val image = if (preset.imageUrl.startsWith(localPrefix)) {
            preset.imageUrl.removePrefix(localPrefix).removeSuffix("?followRedirects=1")
        } else {
            preset.imageUrl
        }
        return setPreset(id = preset.id, name = newName, url = preset.url, image = image)
    }

    suspend fun deletePreset(preset: Preset): Boolean {
        if (ip.isBlank()) return false
        val ok = httpGetOk("http://$ip:11000/SetPreset?id=${preset.id}&delete=1")
        if (ok) refreshPresetsAfterChange()
        return ok
    }

    private suspend fun setPreset(id: Int?, name: String, url: String, image: String): Boolean {
        if (ip.isBlank() || url.isBlank()) return false
        val cleanName = sanitizePresetName(name)
        if (cleanName.isBlank()) return false
        val builder = "http://$ip:11000/SetPreset".toHttpUrl().newBuilder()
        id?.let { builder.addQueryParameter("id", it.toString()) }
        builder.addQueryParameter("name", cleanName)
        builder.addQueryParameter("service", url.substringBefore(':'))
        builder.addQueryParameter("url", url)
        if (image.isNotBlank()) builder.addQueryParameter("image", image)
        val ok = httpGetOk(builder.build().toString())
        if (ok) refreshPresetsAfterChange()
        return ok
    }

    private fun httpGetOk(url: String): Boolean {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Preset request failed: ${e.message}")
            false
        }
    }

    private suspend fun refreshPresetsAfterChange() {
        delay(250)
        fetchPresets()
    }

    /**
     * Käivitab Spotify BluOS seadmel:
     * 1) proovi jätkata olemasolevat sessiooni (/Play)
     * 2) proovi BluOS Capture service kaudu Spotify allikat käivitada
     * 3) proovi Spotify presetit (kui olemas)
     */
    suspend fun startSpotifyOnBlueOs(): Boolean {
        val active = activateSpotify()
        if (active) ensurePlaying()
        return active
    }

    /**
     * Autoplay: kui Spotify on valitud, aga pausil (nt jäi eelmisest korrast pooleli),
     * pane mängima. Kasutaja ei pea eraldi Play-d vajutama.
     */
    private suspend fun ensurePlaying() {
        fetchStatus(longPoll = false)
        if (!_nowPlaying.value.isPlaying) {
            tryResumeSpotify()
        }
    }

    private suspend fun activateSpotify(): Boolean {
        if (ip.isBlank()) return false

        fetchStatus(longPoll = false)
        val initial = _nowPlaying.value
        if (initial.isSpotify) {
            return true
        }

        // Kui mängib TuneIn, siis /Play ei aita ja lisab ainult viidet.
        if (!initial.service.contains("TuneIn", true)) {
            if (tryResumeSpotify()) return true
        }

        val cached = cachedSpotifySourceUrl
        if (!cached.isNullOrBlank() &&
            isPlayableSourceCandidate(cached) &&
            isSpotifySourceCandidate(cached) &&
            playSpotifySource(cached) &&
            waitForSpotifyActivation(attempts = 6, stepMs = 300L)
        ) {
            return true
        }

        var switchTriggered = false

        val spotifySourceCandidates = fetchSpotifySourceCandidates()
        for (sourceUrl in spotifySourceCandidates.take(4)) {
            if (playSpotifySource(sourceUrl)) {
                switchTriggered = true
                if (waitForSpotifyActivation(attempts = 5, stepMs = 300L)) return true
            }
        }

        val spotifyPresetId = findSpotifyPresetId()
        if (spotifyPresetId != null && playPresetInternal(spotifyPresetId)) {
            switchTriggered = true
        }

        if (switchTriggered) {
            if (waitForSpotifyActivation()) return true
        }

        return false
    }

    /**
     * Proovib jÃ¤tkata Spotify sessiooni BluOS /Play kÃ¤suga.
     * Tagastab true kui 2 sekundi pÃ¤rast on Spotify aktiivne.
     */
    suspend fun tryResumeSpotify(): Boolean {
        if (ip.isBlank()) return false
        
        try {
            // Saada /Play kÃ¤sk
            val request = Request.Builder()
                .url("http://$ip:11000/Play")
                .build()
            client.newCall(request).execute().close()
            currentEtag = ""
            
            // Oota natuke et BluOS jÃµuaks reageerida
            delay(450)
            
            // Kontrolli staatust - kas Spotify hakkas mÃ¤ngima?
            fetchStatus(longPoll = false)
            val current = _nowPlaying.value
            return current.isSpotify
        } catch (e: Exception) {
            Log.w(TAG, "Resume Spotify failed: ${e.message}")
            return false
        }
    }

    private suspend fun waitForSpotifyActivation(
        attempts: Int = SPOTIFY_WAIT_ATTEMPTS,
        stepMs: Long = SPOTIFY_WAIT_STEP_MS
    ): Boolean {
        repeat(attempts) {
            delay(stepMs)
            fetchStatus(longPoll = false)
            val current = _nowPlaying.value
            if (current.isSpotify) {
                return true
            }
        }
        return false
    }

    private suspend fun fetchSpotifySourceCandidates(): List<String> {
        if (ip.isBlank()) return emptyList()
        val candidates = LinkedHashSet<String>()
        cachedSpotifySourceUrl
            ?.takeIf { isPlayableSourceCandidate(it) && isSpotifySourceCandidate(it) }
            ?.let { candidates.add(it) }

        // Hard fallback-id enne browse käiku.
        candidates.add("/Play?service=Spotify")
        candidates.add("/Play?url=Spotify%3Aplay")
        candidates.add("/Play?url=Spotify:play")
        candidates.add("Spotify%3Aplay")
        candidates.add("Spotify:play")

        // Kiire tee: loe ainult root Browse ja vajadusel üks tase Spotify key alla.
        val rootItems = fetchBrowseItems("http://$ip:11000/Browse")
        val spotifyRoot = rootItems.firstOrNull { isSpotifyBrowseItem(it, assumeSpotify = false) }
        if (spotifyRoot != null) {
            extractPlayableUrl(spotifyRoot)?.let { candidates.add(it) }
            val key = spotifyRoot.browseKey
            if (key.isNotBlank()) {
                val spotifyItems = fetchBrowseItems(buildBrowseByKeyUrl(key))
                spotifyItems
                    .mapNotNull { extractPlayableUrl(it) }
                    .filter { isSpotifySourceCandidate(it) }
                    .forEach { candidates.add(it) }
            }
        }

        if (candidates.isNotEmpty()) {
            Log.d(TAG, "Spotify source candidates (${candidates.size}): ${candidates.take(6)}")
        } else {
            Log.d(TAG, "Spotify source candidates not found")
        }

        return candidates.toList()
    }

    private suspend fun fetchBrowseItems(url: String): List<BrowseItem> {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val xml = response.body?.string() ?: return emptyList()
                parseBrowseItems(xml)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Browse fetch failed ($url): ${e.message}")
            emptyList()
        }
    }

    private fun parseBrowseItems(xml: String): List<BrowseItem> {
        val items = mutableListOf<BrowseItem>()
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    val id = firstNonBlank(
                        parser.getAttributeValue(null, "id")
                    )
                    val text = firstNonBlank(
                        parser.getAttributeValue(null, "text")
                    )
                    val name = firstNonBlank(
                        parser.getAttributeValue(null, "name")
                    )
                    val title = firstNonBlank(
                        parser.getAttributeValue(null, "title")
                    )
                    val service = firstNonBlank(
                        parser.getAttributeValue(null, "service")
                    )
                    val browseKey = firstNonBlank(
                        parser.getAttributeValue(null, "browseKey"),
                        parser.getAttributeValue(null, "browseId"),
                        parser.getAttributeValue(null, "browseID"),
                        parser.getAttributeValue(null, "key")
                    )
                    val playUrl = firstNonBlank(
                        parser.getAttributeValue(null, "playURL"),
                        parser.getAttributeValue(null, "playUrl"),
                        parser.getAttributeValue(null, "playurl")
                    )
                    val autoplayUrl = firstNonBlank(
                        parser.getAttributeValue(null, "autoplayURL"),
                        parser.getAttributeValue(null, "autoPlayURL"),
                        parser.getAttributeValue(null, "autoplayUrl")
                    )
                    val actionUrl = firstNonBlank(
                        parser.getAttributeValue(null, "actionURL"),
                        parser.getAttributeValue(null, "actionUrl"),
                        parser.getAttributeValue(null, "actionurl")
                    )
                    val url = firstNonBlank(
                        parser.getAttributeValue(null, "URL"),
                        parser.getAttributeValue(null, "url"),
                        parser.getAttributeValue(null, "Url")
                    )

                    if (
                        id.isNotBlank() || text.isNotBlank() || name.isNotBlank() ||
                        title.isNotBlank() || service.isNotBlank() || browseKey.isNotBlank() ||
                        playUrl.isNotBlank() || autoplayUrl.isNotBlank() ||
                        actionUrl.isNotBlank() || url.isNotBlank()
                    ) {
                        items.add(
                            BrowseItem(
                                id = id,
                                text = text,
                                name = name,
                                title = title,
                                service = service,
                                browseKey = browseKey,
                                playUrl = playUrl,
                                autoplayUrl = autoplayUrl,
                                actionUrl = actionUrl,
                                url = url,
                                text2 = firstNonBlank(parser.getAttributeValue(null, "text2")),
                                image = firstNonBlank(parser.getAttributeValue(null, "image"))
                            )
                        )
                    }
                }
                parser.next()
            }
            items
        } catch (e: Exception) {
            Log.w(TAG, "Browse parse failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun playSpotifySource(sourceUrl: String): Boolean {
        if (ip.isBlank() || sourceUrl.isBlank()) return false
        return try {
            val requestUrls = buildSpotifyPlayRequestUrls(sourceUrl)
            if (requestUrls.isEmpty()) return false

            for (requestUrl in requestUrls) {
                val request = Request.Builder().url(requestUrl).build()
                val ok = client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
                if (ok) {
                    currentEtag = ""
                    cachedSpotifySourceUrl = sourceUrl
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Spotify source play failed: ${e.message}")
            false
        }
    }

    private fun buildSpotifyPlayRequestUrls(sourceUrl: String): List<String> {
        if (ip.isBlank() || sourceUrl.isBlank()) return emptyList()
        if (looksLikeApiUrl(sourceUrl)) {
            val normalized = normalizeApiUrl(sourceUrl)
            return if (isPlayableApiEndpoint(normalized)) listOf(normalized) else emptyList()
        }

        val urls = LinkedHashSet<String>()
        val base = "http://$ip:11000/Play".toHttpUrl()

        // Variant A: allikas on juba URL-encoded (nt Spotify%3Aplay)
        if (sourceUrl.contains('%')) {
            urls.add(
                base.newBuilder()
                    .addEncodedQueryParameter("url", sourceUrl)
                    .build()
                    .toString()
            )
            // Variant B: mõni BluOS firmware ootab topeltkodeeringut query sees.
            urls.add(
                base.newBuilder()
                    .addQueryParameter("url", sourceUrl)
                    .build()
                    .toString()
            )
        } else {
            urls.add(
                base.newBuilder()
                    .addQueryParameter("url", sourceUrl)
                    .build()
                    .toString()
            )
        }

        return urls.toList()
    }

    private suspend fun playBrowseUrl(rawUrl: String): Boolean {
        if (ip.isBlank() || rawUrl.isBlank()) return false
        return try {
            val requestUrls = buildGenericPlayRequestUrls(rawUrl)
            if (requestUrls.isEmpty()) return false

            for (requestUrl in requestUrls) {
                val request = Request.Builder().url(requestUrl).build()
                val ok = client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
                if (ok) {
                    currentEtag = ""
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Browse play failed: ${e.message}")
            false
        }
    }

    private fun buildGenericPlayRequestUrls(sourceUrl: String): List<String> {
        if (ip.isBlank() || sourceUrl.isBlank()) return emptyList()
        if (looksLikeApiUrl(sourceUrl)) {
            return listOf(normalizeApiUrl(sourceUrl))
        }
        if (sourceUrl.startsWith("http://", true) || sourceUrl.startsWith("https://", true)) {
            return listOf(sourceUrl)
        }

        val base = "http://$ip:11000/Play".toHttpUrl()
        return listOf(
            base.newBuilder()
                .addQueryParameter("url", sourceUrl)
                .build()
                .toString()
        )
    }

    private data class BrowseItem(
        val id: String,
        val text: String,
        val name: String,
        val title: String,
        val service: String,
        val browseKey: String,
        val playUrl: String,
        val autoplayUrl: String,
        val actionUrl: String,
        val url: String,
        val text2: String = "",
        val image: String = ""
    )

    private data class StatusAction(
        val name: String,
        val text: String,
        val url: String
    )

    private fun BrowseItem.toBrowseEntry(): BrowseEntry {
        val titleText = firstNonBlank(text, name, title, service, id)
        val subtitleText = when {
            text2.isNotBlank() -> text2
            service.isNotBlank() && !service.equals(titleText, true) -> service
            else -> ""
        }
        return BrowseEntry(
            title = titleText,
            subtitle = subtitleText,
            browseKey = browseKey,
            playUrl = playUrl,
            autoplayUrl = autoplayUrl,
            actionUrl = actionUrl,
            imageUrl = image
        )
    }

    private fun isSpotifyBrowseItem(item: BrowseItem, assumeSpotify: Boolean): Boolean {
        if (assumeSpotify) return true
        val haystack = listOf(
            item.id, item.text, item.name, item.title, item.service,
            item.browseKey, item.playUrl, item.autoplayUrl, item.actionUrl, item.url
        ).joinToString("|")
        return haystack.contains("spotify", true)
    }

    private fun isTuneInBrowseItem(item: BrowseItem): Boolean {
        val haystack = listOf(
            item.id, item.text, item.name, item.title, item.service
        ).joinToString("|")
        return haystack.contains("tunein", true) ||
            haystack.contains("radio", true)
    }

    private fun extractPlayableUrl(item: BrowseItem): String? {
        val candidates = listOf(item.playUrl, item.autoplayUrl, item.actionUrl, item.url)
        for (candidate in candidates) {
            if (isPlayableSourceCandidate(candidate)) return candidate
        }
        return null
    }

    private fun buildBrowseByKeyUrl(key: String): String {
        return "http://$ip:11000/Browse".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", key)
            .build()
            .toString()
    }

    private fun looksLikeApiUrl(url: String): Boolean {
        return url.startsWith("/") ||
            url.startsWith("http://$ip:11000", true) ||
            url.startsWith("https://$ip:11000", true) ||
            url.contains(":11000/")
    }

    private fun normalizeApiUrl(url: String): String {
        return when {
            url.startsWith("http://", true) || url.startsWith("https://", true) -> url
            url.startsWith("/") -> "http://$ip:11000$url"
            else -> "http://$ip:11000/$url"
        }
    }

    private fun isPlayableApiEndpoint(url: String): Boolean {
        return url.contains(":11000/Play", true) ||
            url.contains("/Play?", true)
    }

    private fun isPlayableSourceCandidate(source: String): Boolean {
        val value = source.trim()
        if (value.isBlank()) return false
        if (value.contains("Browse", true) || value.contains("Services", true)) return false
        if (value.startsWith("/Play", true) || value.startsWith("Play?", true)) return true
        if (value.contains("%3A", true)) return true
        if (value.startsWith("spotify", true)) return true
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
            return isPlayableApiEndpoint(value)
        }
        // Internal BluOS URL scheme, nt "Spotify:play" vÃµi "Capture:optical"
        return value.contains(':')
    }

    private fun isSpotifySourceCandidate(source: String): Boolean {
        val value = source.trim()
        if (value.isBlank()) return false
        return value.contains("spotify", true) ||
            value.contains("Spotify%3A", true) ||
            value.contains("service=Spotify", true)
    }

    private fun firstNonBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim() ?: ""
    }

    // Ainult BluOS-i "Add preset" tegevus. NB: "Favourite" on TuneIn-i konto lemmik
    // (teine asi) ja /Save salvestab playlisti — neid siin kasutada ei tohi.
    private fun isSavePresetAction(action: StatusAction): Boolean {
        val url = action.url.lowercase()
        return url.contains("action=addpreset") || url.contains("/setpreset")
    }

    private fun sanitizePresetName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.take(60)
    }

    private suspend fun findSpotifyPresetId(): Int? {
        val cached = cachedSpotifyPresetId
        if (cached != null) {
            if (_presets.value.any { it.id == cached }) return cached
            fetchPresets()
            if (_presets.value.any { it.id == cached }) return cached
        }

        val existing = _presets.value.firstOrNull { isSpotifyPreset(it) }?.id
        if (existing != null) {
            cachedSpotifyPresetId = existing
            return existing
        }

        fetchPresets()
        val fetched = _presets.value.firstOrNull { isSpotifyPreset(it) }?.id
        if (fetched != null) {
            cachedSpotifyPresetId = fetched
        }
        return fetched
    }

    private fun isSpotifyPreset(preset: Preset): Boolean {
        return preset.name.contains("spotify", true) ||
            preset.url.contains("spotify", true) ||
            preset.imageUrl.contains("spotify", true)
    }

    /**
     * "Raadio" virtuaalne sisend: mängi viimati kuulatud jaam.
     * Eelistab sama URL-iga lemmikut (/Preset — kõige kindlam), siis otsingust saadud
     * /Play URL-i, viimase võimalusena /Play?url=<jaama URL>.
     */
    suspend fun playRadio(radio: LastRadio): Boolean {
        if (ip.isBlank()) return false
        if (_presets.value.isEmpty()) fetchPresets()
        val preset = if (radio.url.isBlank()) null else _presets.value.firstOrNull {
            it.url == radio.url || it.url.startsWith("${radio.url}/") || radio.url.startsWith("${it.url}/")
        }
        if (preset != null && playPresetInternal(preset.id)) return true
        if (radio.playUrl.isNotBlank() && playBrowseUrl(radio.playUrl)) return true
        return radio.url.isNotBlank() && playBrowseUrl(radio.url)
    }

    private suspend fun playPresetInternal(presetId: Int): Boolean {
        if (ip.isBlank()) return false
        return try {
            val request = Request.Builder()
                .url("http://$ip:11000/Preset?id=$presetId")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    currentEtag = ""
                    if (isSpotifyPreset(_presets.value.firstOrNull { it.id == presetId } ?: Preset())) {
                        cachedSpotifyPresetId = presetId
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun sendCommand(cmd: String) {
        if (ip.isBlank()) return
        
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("http://$ip:11000/$cmd")
                    .build()
                client.newCall(request).execute().close()
                
                // Reset etag et long-polling saaks uue staatuse kohe
                currentEtag = ""
            } catch (e: Exception) {
                Log.w(TAG, "Command $cmd failed: ${e.message}")
            }
        }
    }

    fun refresh() {
        scope.launch {
            currentEtag = ""
            fetchStatus(longPoll = false)
            fetchPresets()
        }
    }

    fun close() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        longPollClient.dispatcher.executorService.shutdown()
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// Data Classes
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

data class NowPlaying(
    val isPlaying: Boolean = false,
    // BluOS-i toores olek: play, pause, stop, stream, connecting
    val state: String = "",
    // BluOS-i voo formaat, nt "MP3 320 kb/s" (tuleb otse NAD-ist)
    val streamFormat: String = "",
    // BluOS-i allika URL, raadio puhul nt "Airable:radio:https://..." (lemmiku-vormis)
    val streamUrl: String = "",
    val service: String = "",
    val serviceIcon: String = "",
    // Muusika (Spotify, Local jne)
    val artist: String = "",
    val track: String = "",
    val album: String = "",
    // Raadio (TuneIn)
    val stationName: String = "",
    val showName: String = "",
    // Pilt
    val imageUrl: String = "",
    // Playback vÃµimalused
    val canSkip: Boolean = false,
    val canSeek: Boolean = false,
    val canSavePreset: Boolean = false,
    val totalSeconds: Int = 0,
    val currentSeconds: Int = 0,
    val isStream: Boolean = false
) {
    val hasContent: Boolean
        get() = track.isNotBlank() || stationName.isNotBlank() || showName.isNotBlank()

    /** "MP3 320 kb/s" -> "MP3 · 320 kbps"; muu formaat näidatakse nii nagu BluOS selle annab. */
    val qualityLabel: String
        get() {
            val match = Regex("""^\s*([A-Za-z0-9+]+)\s+(\d+)\s*kb/?s\s*$""", RegexOption.IGNORE_CASE)
                .find(streamFormat) ?: return streamFormat.trim()
            return "${match.groupValues[1].uppercase()} · ${match.groupValues[2]} kbps"
        }
    
    val isRadio: Boolean
        get() = isStream || service.contains("TuneIn", true) || 
                service.contains("Radio", true) ||
                (stationName.isNotBlank() && !isSpotify)
    
    val isSpotify: Boolean
        get() = service.contains("Spotify", true) ||
            serviceIcon.contains("spotify", true) ||
            stationName.contains("Spotify", true)
    
    /** On-demand muusika (Spotify, Deezer, TIDAL jne) */
    val isOnDemand: Boolean
        get() = canSkip || isSpotify ||
                service.contains("Deezer", true) ||
                service.contains("Tidal", true) ||
                service.contains("Amazon", true)
    
    val displayTitle: String
        get() = when {
            isRadio && !isSpotify -> showName.ifBlank { stationName }
            else -> track
        }
    
    val displaySubtitle: String
        get() = when {
            isRadio && !isSpotify -> artist.ifBlank {
                if (showName.isNotBlank()) stationName else ""
            }
            else -> artist
        }
    
    /** Kolmas rida - album muusika puhul, raadio puhul jaama nimi/extra text */
    val displayAlbum: String
        get() = when {
            !isRadio -> album
            isRadio && !isSpotify -> {
                when {
                    album.isNotBlank() &&
                        !album.equals(displayTitle, true) &&
                        !album.equals(displaySubtitle, true) -> album
                    stationName.isNotBlank() &&
                        !stationName.equals(displayTitle, true) &&
                        !stationName.equals(displaySubtitle, true) -> stationName
                    else -> ""
                }
            }
            else -> ""
        }
}

data class Preset(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
    val imageUrl: String = ""
)

/**
 * Jaama nime võti duplikaatide leidmiseks eri kataloogide ja lemmikute vahel:
 * "Radio Elmar" == "Raadio Elmar", "101.6 | ERR Raadio 2" -> "errradio2",
 * "Raadio Kuku 100.7 (Adult Hits)" == "Raadio Kuku".
 */
fun radioNameKey(name: String): String {
    var s = name.lowercase()
    if (s.contains("|")) s = s.substringAfterLast("|")
    return s
        .replace(Regex("""\(.*?\)"""), " ")
        .replace(Regex("""\b\d{2,3}[.,]\d\b"""), " ")
        .replace("raadio", "radio")
        .replace(Regex("""[^\p{L}\p{N}]"""), "")
}

/** Raadiovoo kvaliteet, nt AAC 192 kbps. */
data class StreamQuality(val format: String, val kbps: Int) {
    /** TuneIn annab nt 191; näitame lähimat tavapärast väärtust (192). */
    val label: String
        get() {
            val standard = listOf(32, 48, 64, 96, 128, 160, 192, 256, 320)
                .minByOrNull { kotlin.math.abs(it - kbps) }
                ?.takeIf { kotlin.math.abs(it - kbps) <= it * 0.05 } ?: kbps
            return listOf(format, "$standard kbps").filter { it.isNotBlank() }.joinToString(" · ")
        }
}

private val UNKNOWN_QUALITY = StreamQuality("", 0)

data class BrowseEntry(
    val title: String = "",
    val subtitle: String = "",
    val browseKey: String = "",
    val playUrl: String = "",
    val autoplayUrl: String = "",
    val actionUrl: String = "",
    // Jaama logo (TuneIn)
    val imageUrl: String = ""
) {
    val isBrowsable: Boolean
        get() = browseKey.isNotBlank()

    val isPlayable: Boolean
        get() = playUrl.isNotBlank() || autoplayUrl.isNotBlank() || actionUrl.isNotBlank()
}


