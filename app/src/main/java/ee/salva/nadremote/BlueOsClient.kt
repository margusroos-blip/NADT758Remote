package com.nadremote.app

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        private const val SPOTIFY_WAIT_ATTEMPTS = 7
        private const val SPOTIFY_WAIT_STEP_MS = 900L
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
            
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "status") {
                            etag = parser.getAttributeValue(null, "etag") ?: ""
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
                service = service,
                serviceIcon = serviceIcon,
                artist = artist.ifBlank { title2 },
                track = title1,
                album = album.ifBlank { title3 },
                stationName = name,
                showName = if (hasStreamUrl || service.contains("TuneIn", true)) title1 else "",
                imageUrl = when {
                    image.startsWith("http") -> image
                    image.isNotBlank() -> "http://$ip:11000$image?followRedirects=1"
                    else -> ""
                },
                canSkip = !hasStreamUrl,  // Skip/Back ainult kui play queue on allikas
                canSeek = canSeek,
                totalSeconds = totlen,
                currentSeconds = secs,
                isStream = hasStreamUrl
            )
            
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
     * Käivitab Spotify BluOS seadmel:
     * 1) proovi jätkata olemasolevat sessiooni (/Play)
     * 2) proovi BluOS Capture service kaudu Spotify allikat käivitada
     * 3) proovi Spotify presetit (kui olemas)
     */
    suspend fun startSpotifyOnBlueOs(): Boolean {
        if (ip.isBlank()) return false

        fetchStatus(longPoll = false)
        if (_nowPlaying.value.isSpotify && (_nowPlaying.value.isPlaying || _nowPlaying.value.track.isNotBlank())) {
            return true
        }

        if (tryResumeSpotify()) return true

        val spotifySourceUrl = fetchSpotifySourceUrl()
        if (spotifySourceUrl != null && playSpotifySource(spotifySourceUrl) && waitForSpotifyActivation()) {
            return true
        }

        val spotifyPresetId = findSpotifyPresetId()
        if (spotifyPresetId != null && playPresetInternal(spotifyPresetId) && waitForSpotifyActivation()) {
            return true
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
            delay(1500)
            
            // Kontrolli staatust - kas Spotify hakkas mÃ¤ngima?
            fetchStatus(longPoll = false)
            val current = _nowPlaying.value
            return current.isSpotify && (current.isPlaying || current.track.isNotBlank())
        } catch (e: Exception) {
            Log.w(TAG, "Resume Spotify failed: ${e.message}")
            return false
        }
    }

    private suspend fun waitForSpotifyActivation(): Boolean {
        repeat(SPOTIFY_WAIT_ATTEMPTS) {
            delay(SPOTIFY_WAIT_STEP_MS)
            fetchStatus(longPoll = false)
            val current = _nowPlaying.value
            if (current.isSpotify || current.service.contains("Spotify", true)) {
                return true
            }
        }
        return false
    }

    private suspend fun fetchSpotifySourceUrl(): String? {
        if (ip.isBlank()) return null
        val endpoints = listOf(
            "http://$ip:11000/Browse" to false,
            "http://$ip:11000/RadioBrowse?service=Capture" to false,
            "http://$ip:11000/Browse?service=Capture" to false,
            "http://$ip:11000/Browse?service=Spotify" to true,
            "http://$ip:11000/RadioBrowse?service=Spotify" to true
        )

        val visitedKeys = mutableSetOf<String>()
        for ((url, assumeSpotify) in endpoints) {
            val candidate = fetchSpotifySourceFromEndpoint(
                url = url,
                assumeSpotify = assumeSpotify,
                visitedKeys = visitedKeys,
                depth = 0
            )
            if (!candidate.isNullOrBlank()) {
                cachedSpotifySourceUrl = candidate
                return candidate
            }
        }

        return cachedSpotifySourceUrl
    }

    private suspend fun fetchSpotifySourceFromEndpoint(
        url: String,
        assumeSpotify: Boolean,
        visitedKeys: MutableSet<String>,
        depth: Int
    ): String? {
        if (depth > 2) return null
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val xml = response.body?.string() ?: return null
                val items = parseBrowseItems(xml)
                val direct = pickSpotifyPlayableUrl(items, assumeSpotify)
                if (!direct.isNullOrBlank()) {
                    return direct
                }

                val browseKeys = items.asSequence()
                    .filter { isSpotifyBrowseItem(it, assumeSpotify) || assumeSpotify }
                    .mapNotNull { it.browseKey.takeIf { key -> key.isNotBlank() } }
                    .distinct()
                    .take(8)
                    .toList()

                for (key in browseKeys) {
                    if (!visitedKeys.add(key)) continue
                    val browseUrl = buildBrowseByKeyUrl(key)
                    val nested = fetchSpotifySourceFromEndpoint(
                        url = browseUrl,
                        assumeSpotify = true,
                        visitedKeys = visitedKeys,
                        depth = depth + 1
                    )
                    if (!nested.isNullOrBlank()) {
                        return nested
                    }
                }

                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Spotify source fetch failed ($url): ${e.message}")
            null
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
                    val url = firstNonBlank(
                        parser.getAttributeValue(null, "URL"),
                        parser.getAttributeValue(null, "url"),
                        parser.getAttributeValue(null, "Url")
                    )

                    if (
                        id.isNotBlank() || text.isNotBlank() || name.isNotBlank() ||
                        title.isNotBlank() || service.isNotBlank() || browseKey.isNotBlank() ||
                        playUrl.isNotBlank() || autoplayUrl.isNotBlank() || url.isNotBlank()
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
                                url = url
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
            val requestUrl = if (looksLikeApiUrl(sourceUrl)) {
                normalizeApiUrl(sourceUrl)
            } else {
                val playUrlBuilder = "http://$ip:11000/Play".toHttpUrl().newBuilder()
                if (sourceUrl.contains('%')) {
                    playUrlBuilder.addEncodedQueryParameter("url", sourceUrl)
                } else {
                    playUrlBuilder.addQueryParameter("url", sourceUrl)
                }
                playUrlBuilder.build().toString()
            }

            val request = Request.Builder().url(requestUrl).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    currentEtag = ""
                    cachedSpotifySourceUrl = sourceUrl
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Spotify source play failed: ${e.message}")
            false
        }
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
        val url: String
    )

    private fun pickSpotifyPlayableUrl(items: List<BrowseItem>, assumeSpotify: Boolean): String? {
        for (item in items) {
            val spotifyItem = isSpotifyBrowseItem(item, assumeSpotify)
            if (!spotifyItem && !assumeSpotify) continue
            val candidate = extractPlayableUrl(item)
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }
        return null
    }

    private fun isSpotifyBrowseItem(item: BrowseItem, assumeSpotify: Boolean): Boolean {
        if (assumeSpotify) return true
        val haystack = listOf(
            item.id, item.text, item.name, item.title, item.service,
            item.browseKey, item.playUrl, item.autoplayUrl, item.url
        ).joinToString("|")
        return haystack.contains("spotify", true)
    }

    private fun extractPlayableUrl(item: BrowseItem): String? {
        val direct = firstNonBlank(item.playUrl, item.autoplayUrl)
        if (direct.isNotBlank()) return direct
        val source = item.url
        if (source.isNotBlank()) return source
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

    private fun firstNonBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim() ?: ""
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
    val totalSeconds: Int = 0,
    val currentSeconds: Int = 0,
    val isStream: Boolean = false
) {
    val hasContent: Boolean
        get() = track.isNotBlank() || stationName.isNotBlank()
    
    val isRadio: Boolean
        get() = isStream || service.contains("TuneIn", true) || 
                service.contains("Radio", true) ||
                (stationName.isNotBlank() && !isSpotify)
    
    val isSpotify: Boolean
        get() = service.contains("Spotify", true)
    
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
            isRadio && !isSpotify -> if (showName.isNotBlank()) stationName else ""
            else -> artist
        }
    
    /** Kolmas rida - album Spotify/muusika puhul */
    val displayAlbum: String
        get() = if (!isRadio) album else ""
}

data class Preset(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
    val imageUrl: String = ""
)


