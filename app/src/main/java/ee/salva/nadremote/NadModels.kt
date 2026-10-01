package com.nadremote.app

/**
 * NAD T758 v3 Remote - Data Models
 */

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// NAD AVR State
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

data class NadState(
    val model: String = "",
    val power: Boolean = false,
    val mute: Boolean = false,
    val volume: Int = -40,
    val sourceId: Int = 1,
    val sources: Map<Int, String> = emptyMap(),
    val sourcesEnabled: Map<Int, Boolean> = emptyMap()
) {
    val volumeDisplay: String
        get() = "$volume dB"

    val currentSourceName: String
        get() = sources[sourceId] ?: "Source $sourceId"
    
    // Ainult aktiivsed sisendid
    val enabledSources: Map<Int, String>
        get() = sources.filter { (id, _) -> sourcesEnabled[id] == true }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// Connection
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// Discovery
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

data class FoundDevice(
    val name: String,
    val ip: String,
    // BluOS-i MAC-aadress; selle järgi tunneme sama NAD-i ära ka pärast IP muutumist
    val mac: String = ""
)



/**
 * Äpi virtuaalsed sisendid. NAD-i jaoks on need mõlemad BluOS sisend; äpis on need
 * eraldi nupud nagu vana ressiiveri "Tuner" ja "CD". ID-d on väljaspool NAD-i
 * sisendite vahemikku (1..10), et lemmiksisendite nimekirjas segi ei läheks.
 */
object VirtualSource {
    const val RADIO = 101
    const val SPOTIFY = 102

    fun isVirtual(id: Int) = id == RADIO || id == SPOTIFY
}

/** Viimati kuulatud raadiojaam — "Raadio" sisend jätkab sellega. */
data class LastRadio(
    val name: String,
    // Lemmiku-vormis URL, nt "TuneIn:s25067" või "Airable:radio:https://..."
    val url: String,
    val imageUrl: String = "",
    // BluOS-i /Play URL, kui jaam valiti otsingust (kõige kindlam viis uuesti mängida)
    val playUrl: String = ""
)
