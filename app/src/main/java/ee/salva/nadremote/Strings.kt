package com.nadremote.app

/**
 * Lokaliseeritud tekstid
 */
object Strings {
    
    fun get(language: AppLanguage): StringResources {
        return when (language) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.ESTONIAN -> EstonianStrings
            AppLanguage.GERMAN -> GermanStrings
            AppLanguage.FINNISH -> FinnishStrings
            AppLanguage.SWEDISH -> SwedishStrings
            AppLanguage.NORWEGIAN -> NorwegianStrings
            AppLanguage.DANISH -> DanishStrings
            AppLanguage.FRENCH -> FrenchStrings
            AppLanguage.ITALIAN -> ItalianStrings
            AppLanguage.SPANISH -> SpanishStrings
        }
    }
}

interface StringResources {
    // App
    val appName: String
    
    // Settings
    val settings: String
    val language: String
    val theme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val deviceConnection: String
    val deviceIp: String
    val deviceIpPlaceholder: String
    val connect: String
    val disconnect: String
    val discoveredDevices: String
    val scanNetwork: String
    val scanning: String
    val noDevicesFound: String
    val clearSavedDevice: String
    val findNad: String
    val searchingNad: String
    val nadNotFound: String
    val sameWifiHint: String
    val enterIpManually: String
    val reorderHint: String
    val spotifyNoAudio: String
    val openSpotifyApp: String
    val presetEdit: String
    val presetDone: String
    val presetEditHint: String
    val addCurrentPreset: String
    val renamePreset: String
    val removePresetTitle: String
    val remove: String
    val cancel: String
    val save: String
    val presetActionFailed: String
    val addToPresets: String
    val searchStations: String
    val localRadio: String
    val browseAllCategories: String
    val noResults: String
    val playFailed: String
    val radio: String
    val virtualSourceHint: String
    val chooseStation: String
    val radioFavorites: String
    val radioReorderHint: String
    val advancedSettings: String
    val autoConnect: String
    val autoConnectDesc: String
    val manualIp: String
    val connectedTo: String
    val tapToConnect: String
    
    // Favorite sources
    val favoriteSources: String
    val favoriteSourcesDesc: String
    val selectUpTo4: String
    val noSourcesAvailable: String
    
    // Now Playing
    val nowPlaying: String
    val nothingPlaying: String
    
    // Presets
    val presets: String
    val noPresets: String
    val favoritePresetsDesc: String
    val selectUpTo3: String
    
    // Services
    val services: String
    
    // Remote
    val power: String
    val input: String
    val volume: String
    val muted: String
    
    // Connection status
    val connected: String
    val connecting: String
    val disconnected: String
    val error: String
    val notConnected: String
    val openSettings: String
    val tryAgain: String
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// ENGLISH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object EnglishStrings : StringResources {
    override val appName = "NAD Remote"
    
    override val settings = "Settings"
    override val language = "Language"
    override val theme = "Theme"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val deviceConnection = "Device"
    override val deviceIp = "Device IP Address"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Connect"
    override val disconnect = "Disconnect"
    override val discoveredDevices = "Discovered Devices"
    override val scanNetwork = "Scan"
    override val scanning = "Scanning..."
    override val noDevicesFound = "No devices found. Tap Scan to search."
    override val clearSavedDevice = "Forget Device"
    override val findNad = "Find NAD"
    override val searchingNad = "Looking for NAD…"
    override val nadNotFound = "NAD not found"
    override val sameWifiHint = "Phone and NAD must be on the same Wi-Fi network."
    override val enterIpManually = "Enter IP manually"
    override val reorderHint = "Long press and drag icons. Top 3 appear on main screen."
    override val spotifyNoAudio = "Spotify isn’t getting audio. In Spotify, switch to “This phone” for a moment, then back to NAD."
    override val openSpotifyApp = "Open Spotify"
    override val presetEdit = "Edit"
    override val presetDone = "Done"
    override val presetEditHint = "Tap a favourite to rename it. ✕ removes it."
    override val addCurrentPreset = "Add now playing to favourites"
    override val renamePreset = "Rename"
    override val removePresetTitle = "Remove favourite?"
    override val remove = "Remove"
    override val cancel = "Cancel"
    override val save = "Save"
    override val presetActionFailed = "Didn’t work. Please try again."
    override val addToPresets = "Add to favourites"
    override val searchStations = "Search radio stations"
    override val localRadio = "Local radio"
    override val browseAllCategories = "Browse all categories"
    override val noResults = "Nothing found"
    override val playFailed = "Couldn’t start this station"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio and Spotify use the receiver’s BluOS input. Radio resumes the station you listened to last."
    override val chooseStation = "Choose a station"
    override val radioFavorites = "Radio favourites"
    override val radioReorderHint = "Long press and drag to change the order."
    override val advancedSettings = "Advanced"
    override val autoConnect = "Auto-connect"
    override val autoConnectDesc = "Connect automatically when app opens"
    override val manualIp = "Manual IP"
    override val connectedTo = "Connected to"
    override val tapToConnect = "Tap to find your NAD receiver"
    
    override val favoriteSources = "Favorite Inputs"
    override val favoriteSourcesDesc = "Choose up to 4 inputs to show on home screen"
    override val selectUpTo4 = "Select up to 4"
    override val noSourcesAvailable = "Connect to device first"
    
    override val nowPlaying = "Now Playing"
    override val nothingPlaying = "Nothing playing"
    
    override val presets = "Presets"
    override val noPresets = "No presets saved"
    override val favoritePresetsDesc = "Choose up to 3 presets to show on home screen"
    override val selectUpTo3 = "Select up to 3"
    
    override val services = "SERVICES"
    
    override val power = "POWER"
    override val input = "INPUT"
    override val volume = "VOLUME"
    override val muted = "MUTED"
    
    override val connected = "Connected"
    override val connecting = "Connecting..."
    override val disconnected = "Disconnected"
    override val error = "Error"
    override val notConnected = "Not Connected"
    override val openSettings = "Settings"
    override val tryAgain = "Try Again"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// ESTONIAN
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object EstonianStrings : StringResources {
    override val appName = "NAD Pult"
    
    override val settings = "Seaded"
    override val language = "Keel"
    override val theme = "Teema"
    override val themeSystem = "Süsteemi"
    override val themeLight = "Hele"
    override val themeDark = "Tume"
    override val deviceConnection = "Seade"
    override val deviceIp = "Seadme IP-aadress"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Ühenda"
    override val disconnect = "Katkesta"
    override val discoveredDevices = "Leitud seadmed"
    override val scanNetwork = "Otsi"
    override val scanning = "Otsin..."
    override val noDevicesFound = "Seadmeid pole leitud. Vajuta Otsi."
    override val clearSavedDevice = "Unusta seade"
    override val findNad = "Otsi NAD"
    override val searchingNad = "Otsin NAD-i…"
    override val nadNotFound = "NAD-i ei leitud"
    override val sameWifiHint = "Telefon ja NAD peavad olema samas Wi-Fi võrgus."
    override val enterIpManually = "Sisesta IP käsitsi"
    override val reorderHint = "Hoia ikoonil sõrme ja lohista. Esimesed 3 on põhiekraanil."
    override val spotifyNoAudio = "Spotify ei saa heli kätte. Vali Spotify äpis korraks „See telefon“ ja siis uuesti NAD."
    override val openSpotifyApp = "Ava Spotify"
    override val presetEdit = "Muuda"
    override val presetDone = "Valmis"
    override val presetEditHint = "Vajuta lemmikule, et nime muuta. ✕ eemaldab lemmiku."
    override val addCurrentPreset = "Lisa praegu mängiv lemmikuks"
    override val renamePreset = "Nimeta ümber"
    override val removePresetTitle = "Eemaldada lemmik?"
    override val remove = "Eemalda"
    override val cancel = "Tühista"
    override val save = "Salvesta"
    override val presetActionFailed = "Ei õnnestunud. Proovi uuesti."
    override val addToPresets = "Lisa lemmikuks"
    override val searchStations = "Otsi raadiojaama"
    override val localRadio = "Kohalikud raadiod"
    override val browseAllCategories = "Sirvi kõiki kategooriaid"
    override val noResults = "Midagi ei leitud"
    override val playFailed = "Jaama ei õnnestunud mängima panna"
    override val radio = "Raadio"
    override val virtualSourceHint = "Raadio ja Spotify kasutavad võimendi BluOS sisendit. Raadio jätkab viimati kuulatud jaamaga."
    override val chooseStation = "Vali jaam"
    override val radioFavorites = "Raadio lemmikud"
    override val radioReorderHint = "Hoia jaamal sõrme ja lohista, et järjekorda muuta."
    override val advancedSettings = "Täpsemalt"
    override val autoConnect = "Automaatühendus"
    override val autoConnectDesc = "Ühenda automaatselt rakenduse avamisel"
    override val manualIp = "Käsitsi IP"
    override val connectedTo = "Ühendatud"
    override val tapToConnect = "Vajuta, et leida NAD vastuvõtja"
    
    override val favoriteSources = "Lemmik sisendid"
    override val favoriteSourcesDesc = "Vali kuni 4 sisendit avalehele"
    override val selectUpTo4 = "Vali kuni 4"
    override val noSourcesAvailable = "Ühenda esmalt seadmega"
    
    override val nowPlaying = "Praegu mängib"
    override val nothingPlaying = "Midagi ei mängi"
    
    override val presets = "Lemmikud"
    override val noPresets = "Lemmikuid pole salvestatud"
    override val favoritePresetsDesc = "Vali kuni 3 presetit avalehele"
    override val selectUpTo3 = "Vali kuni 3"
    
    override val services = "TEENUSED"
    
    override val power = "TOIDE"
    override val input = "SISEND"
    override val volume = "HELITUGEVUS"
    override val muted = "VAIGISTATUD"
    
    override val connected = "Ühendatud"
    override val connecting = "Ühendamine..."
    override val disconnected = "Ühendamata"
    override val error = "Viga"
    override val notConnected = "Pole ühendatud"
    override val openSettings = "Seaded"
    override val tryAgain = "Proovi uuesti"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// GERMAN
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object GermanStrings : StringResources {
    override val appName = "NAD Fernbedienung"
    
    override val settings = "Einstellungen"
    override val language = "Sprache"
    override val theme = "Design"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val deviceConnection = "GerÃ¤t"
    override val deviceIp = "GerÃ¤te-IP-Adresse"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Verbinden"
    override val disconnect = "Trennen"
    override val discoveredDevices = "Gefundene GerÃ¤te"
    override val scanNetwork = "Suchen"
    override val scanning = "Suche..."
    override val noDevicesFound = "Keine GerÃ¤te gefunden. Tippe auf Suchen."
    override val clearSavedDevice = "GerÃ¤t vergessen"
    override val findNad = "NAD suchen"
    override val searchingNad = "Suche NAD…"
    override val nadNotFound = "NAD nicht gefunden"
    override val sameWifiHint = "Telefon und NAD müssen im selben WLAN sein."
    override val enterIpManually = "IP manuell eingeben"
    override val reorderHint = "Symbol gedrückt halten und ziehen. Die ersten 3 erscheinen auf dem Hauptbildschirm."
    override val spotifyNoAudio = "Spotify empfängt keinen Ton. Wähle in Spotify kurz „Dieses Telefon“ und dann wieder NAD."
    override val openSpotifyApp = "Spotify öffnen"
    override val presetEdit = "Bearbeiten"
    override val presetDone = "Fertig"
    override val presetEditHint = "Tippe auf einen Favoriten, um ihn umzubenennen. ✕ entfernt ihn."
    override val addCurrentPreset = "Aktuellen Titel zu Favoriten hinzufügen"
    override val renamePreset = "Umbenennen"
    override val removePresetTitle = "Favorit entfernen?"
    override val remove = "Entfernen"
    override val cancel = "Abbrechen"
    override val save = "Speichern"
    override val presetActionFailed = "Hat nicht geklappt. Bitte erneut versuchen."
    override val addToPresets = "Zu Favoriten hinzufügen"
    override val searchStations = "Radiosender suchen"
    override val localRadio = "Lokale Sender"
    override val browseAllCategories = "Alle Kategorien durchsuchen"
    override val noResults = "Nichts gefunden"
    override val playFailed = "Sender konnte nicht gestartet werden"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio und Spotify nutzen den BluOS-Eingang des Receivers. Radio setzt mit dem zuletzt gehörten Sender fort."
    override val chooseStation = "Sender wählen"
    override val radioFavorites = "Radio-Favoriten"
    override val radioReorderHint = "Gedrückt halten und ziehen, um die Reihenfolge zu ändern."
    override val advancedSettings = "Erweitert"
    override val autoConnect = "Auto-Verbindung"
    override val autoConnectDesc = "Automatisch verbinden beim Ã–ffnen der App"
    override val manualIp = "Manuelle IP"
    override val connectedTo = "Verbunden mit"
    override val tapToConnect = "Tippe, um deinen NAD-Receiver zu finden"
    
    override val favoriteSources = "Favoriten-EingÃ¤nge"
    override val favoriteSourcesDesc = "WÃ¤hle bis zu 4 EingÃ¤nge fÃ¼r den Startbildschirm"
    override val selectUpTo4 = "Bis zu 4 wÃ¤hlen"
    override val noSourcesAvailable = "Zuerst mit GerÃ¤t verbinden"
    
    override val nowPlaying = "LÃ¤uft gerade"
    override val nothingPlaying = "Nichts wird abgespielt"
    
    override val presets = "Voreinstellungen"
    override val noPresets = "Keine Voreinstellungen gespeichert"
    override val favoritePresetsDesc = "WÃ¤hle bis zu 3 Voreinstellungen fÃ¼r den Startbildschirm"
    override val selectUpTo3 = "Bis zu 3 wÃ¤hlen"
    
    override val services = "DIENSTE"
    
    override val power = "EIN/AUS"
    override val input = "EINGANG"
    override val volume = "LAUTSTÃ„RKE"
    override val muted = "STUMM"
    
    override val connected = "Verbunden"
    override val connecting = "Verbinde..."
    override val disconnected = "Getrennt"
    override val error = "Fehler"
    override val notConnected = "Nicht verbunden"
    override val openSettings = "Einstellungen"
    override val tryAgain = "Erneut versuchen"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// FINNISH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object FinnishStrings : StringResources {
    override val appName = "NAD KaukosÃ¤Ã¤din"
    
    override val settings = "Asetukset"
    override val language = "Kieli"
    override val theme = "Teema"
    override val themeSystem = "JÃ¤rjestelmÃ¤"
    override val themeLight = "Vaalea"
    override val themeDark = "Tumma"
    override val deviceConnection = "Laite"
    override val deviceIp = "Laitteen IP-osoite"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "YhdistÃ¤"
    override val disconnect = "Katkaise"
    override val discoveredDevices = "LÃ¶ydetyt laitteet"
    override val scanNetwork = "Etsi"
    override val scanning = "EtsitÃ¤Ã¤n..."
    override val noDevicesFound = "Laitteita ei lÃ¶ytynyt. Napauta Etsi."
    override val clearSavedDevice = "Unohda laite"
    override val findNad = "Etsi NAD"
    override val searchingNad = "Etsitään NAD:ia…"
    override val nadNotFound = "NAD:ia ei löytynyt"
    override val sameWifiHint = "Puhelimen ja NAD:n on oltava samassa Wi-Fi-verkossa."
    override val enterIpManually = "Syötä IP käsin"
    override val reorderHint = "Paina kuvaketta pitkään ja vedä. Kolme ensimmäistä näkyy päänäkymässä."
    override val spotifyNoAudio = "Spotify ei saa ääntä. Valitse Spotifyssa hetkeksi ”Tämä puhelin” ja sitten taas NAD."
    override val openSpotifyApp = "Avaa Spotify"
    override val presetEdit = "Muokkaa"
    override val presetDone = "Valmis"
    override val presetEditHint = "Napauta suosikkia nimetäksesi sen uudelleen. ✕ poistaa sen."
    override val addCurrentPreset = "Lisää nyt soiva suosikkeihin"
    override val renamePreset = "Nimeä uudelleen"
    override val removePresetTitle = "Poistetaanko suosikki?"
    override val remove = "Poista"
    override val cancel = "Peruuta"
    override val save = "Tallenna"
    override val presetActionFailed = "Ei onnistunut. Yritä uudelleen."
    override val addToPresets = "Lisää suosikkeihin"
    override val searchStations = "Hae radiokanavia"
    override val localRadio = "Paikalliset radiot"
    override val browseAllCategories = "Selaa kaikkia luokkia"
    override val noResults = "Mitään ei löytynyt"
    override val playFailed = "Kanavan käynnistys epäonnistui"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio ja Spotify käyttävät vahvistimen BluOS-tuloa. Radio jatkaa viimeksi kuunnellulla kanavalla."
    override val chooseStation = "Valitse kanava"
    override val radioFavorites = "Radiosuosikit"
    override val radioReorderHint = "Paina pitkään ja vedä muuttaaksesi järjestystä."
    override val advancedSettings = "LisÃ¤asetukset"
    override val autoConnect = "Automaattinen yhteys"
    override val autoConnectDesc = "YhdistÃ¤ automaattisesti sovelluksen avautuessa"
    override val manualIp = "Manuaalinen IP"
    override val connectedTo = "Yhdistetty"
    override val tapToConnect = "Napauta lÃ¶ytÃ¤Ã¤ksesi NAD-vastaanotin"
    
    override val favoriteSources = "Suosikkitulot"
    override val favoriteSourcesDesc = "Valitse enintÃ¤Ã¤n 4 tuloa aloitusnÃ¤yttÃ¶Ã¶n"
    override val selectUpTo4 = "Valitse enintÃ¤Ã¤n 4"
    override val noSourcesAvailable = "YhdistÃ¤ ensin laitteeseen"
    
    override val nowPlaying = "Nyt soi"
    override val nothingPlaying = "Ei toisteta mitÃ¤Ã¤n"
    
    override val presets = "Esiasetukset"
    override val noPresets = "Ei tallennettuja esiasetuksia"
    override val favoritePresetsDesc = "Valitse enintÃ¤Ã¤n 3 esiasetusta aloitusnÃ¤yttÃ¶Ã¶n"
    override val selectUpTo3 = "Valitse enintÃ¤Ã¤n 3"
    
    override val services = "PALVELUT"
    
    override val power = "VIRTA"
    override val input = "TULO"
    override val volume = "Ã„Ã„NENVOIMAKKUUS"
    override val muted = "MYKISTETTY"
    
    override val connected = "Yhdistetty"
    override val connecting = "YhdistetÃ¤Ã¤n..."
    override val disconnected = "Yhteys katkaistu"
    override val error = "Virhe"
    override val notConnected = "Ei yhteyttÃ¤"
    override val openSettings = "Asetukset"
    override val tryAgain = "YritÃ¤ uudelleen"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SWEDISH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object SwedishStrings : StringResources {
    override val appName = "NAD FjÃ¤rrkontroll"
    
    override val settings = "InstÃ¤llningar"
    override val language = "SprÃ¥k"
    override val theme = "Tema"
    override val themeSystem = "System"
    override val themeLight = "Ljust"
    override val themeDark = "MÃ¶rkt"
    override val deviceConnection = "Enhet"
    override val deviceIp = "Enhetens IP-adress"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Anslut"
    override val disconnect = "Koppla frÃ¥n"
    override val discoveredDevices = "Hittade enheter"
    override val scanNetwork = "SÃ¶k"
    override val scanning = "SÃ¶ker..."
    override val noDevicesFound = "Inga enheter hittades. Tryck pÃ¥ SÃ¶k."
    override val clearSavedDevice = "GlÃ¶m enhet"
    override val findNad = "Sök NAD"
    override val searchingNad = "Söker NAD…"
    override val nadNotFound = "NAD hittades inte"
    override val sameWifiHint = "Telefonen och NAD måste vara på samma Wi-Fi-nätverk."
    override val enterIpManually = "Ange IP manuellt"
    override val reorderHint = "Håll in och dra ikonerna. De 3 första visas på huvudskärmen."
    override val spotifyNoAudio = "Spotify får inget ljud. Välj ”Den här telefonen” i Spotify en stund och sedan NAD igen."
    override val openSpotifyApp = "Öppna Spotify"
    override val presetEdit = "Redigera"
    override val presetDone = "Klar"
    override val presetEditHint = "Tryck på en favorit för att byta namn. ✕ tar bort den."
    override val addCurrentPreset = "Lägg till det som spelas i favoriter"
    override val renamePreset = "Byt namn"
    override val removePresetTitle = "Ta bort favorit?"
    override val remove = "Ta bort"
    override val cancel = "Avbryt"
    override val save = "Spara"
    override val presetActionFailed = "Det gick inte. Försök igen."
    override val addToPresets = "Lägg till i favoriter"
    override val searchStations = "Sök radiostationer"
    override val localRadio = "Lokal radio"
    override val browseAllCategories = "Bläddra i alla kategorier"
    override val noResults = "Inget hittades"
    override val playFailed = "Kunde inte starta stationen"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio och Spotify använder receiverns BluOS-ingång. Radio fortsätter med den station du lyssnade på senast."
    override val chooseStation = "Välj en station"
    override val radioFavorites = "Radiofavoriter"
    override val radioReorderHint = "Håll in och dra för att ändra ordningen."
    override val advancedSettings = "Avancerat"
    override val autoConnect = "Auto-anslutning"
    override val autoConnectDesc = "Anslut automatiskt nÃ¤r appen Ã¶ppnas"
    override val manualIp = "Manuell IP"
    override val connectedTo = "Ansluten till"
    override val tapToConnect = "Tryck fÃ¶r att hitta din NAD-mottagare"
    
    override val favoriteSources = "FavoritingÃ¥ngar"
    override val favoriteSourcesDesc = "VÃ¤lj upp till 4 ingÃ¥ngar fÃ¶r startskÃ¤rmen"
    override val selectUpTo4 = "VÃ¤lj upp till 4"
    override val noSourcesAvailable = "Anslut till enheten fÃ¶rst"
    
    override val nowPlaying = "Spelar nu"
    override val nothingPlaying = "Inget spelas"
    
    override val presets = "FÃ¶rinstÃ¤llningar"
    override val noPresets = "Inga sparade fÃ¶rinstÃ¤llningar"
    override val favoritePresetsDesc = "VÃ¤lj upp till 3 fÃ¶rinstÃ¤llningar fÃ¶r startskÃ¤rmen"
    override val selectUpTo3 = "VÃ¤lj upp till 3"
    
    override val services = "TJÃ„NSTER"
    
    override val power = "STRÃ–M"
    override val input = "INGÃ…NG"
    override val volume = "VOLYM"
    override val muted = "TYST"
    
    override val connected = "Ansluten"
    override val connecting = "Ansluter..."
    override val disconnected = "FrÃ¥nkopplad"
    override val error = "Fel"
    override val notConnected = "Inte ansluten"
    override val openSettings = "InstÃ¤llningar"
    override val tryAgain = "FÃ¶rsÃ¶k igen"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// NORWEGIAN
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object NorwegianStrings : StringResources {
    override val appName = "NAD Fjernkontroll"
    
    override val settings = "Innstillinger"
    override val language = "SprÃ¥k"
    override val theme = "Tema"
    override val themeSystem = "System"
    override val themeLight = "Lyst"
    override val themeDark = "MÃ¸rkt"
    override val deviceConnection = "Enhet"
    override val deviceIp = "Enhetens IP-adresse"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Koble til"
    override val disconnect = "Koble fra"
    override val discoveredDevices = "Oppdagede enheter"
    override val scanNetwork = "SÃ¸k"
    override val scanning = "SÃ¸ker..."
    override val noDevicesFound = "Ingen enheter funnet. Trykk pÃ¥ SÃ¸k."
    override val clearSavedDevice = "Glem enhet"
    override val findNad = "Søk etter NAD"
    override val searchingNad = "Søker etter NAD…"
    override val nadNotFound = "Fant ikke NAD"
    override val sameWifiHint = "Telefonen og NAD må være på samme Wi-Fi-nettverk."
    override val enterIpManually = "Skriv inn IP manuelt"
    override val reorderHint = "Hold inne og dra ikonene. De 3 første vises på hovedskjermen."
    override val spotifyNoAudio = "Spotify får ikke lyd. Velg «Denne telefonen» i Spotify et øyeblikk, og deretter NAD igjen."
    override val openSpotifyApp = "Åpne Spotify"
    override val presetEdit = "Rediger"
    override val presetDone = "Ferdig"
    override val presetEditHint = "Trykk på en favoritt for å gi den nytt navn. ✕ fjerner den."
    override val addCurrentPreset = "Legg til det som spilles i favoritter"
    override val renamePreset = "Gi nytt navn"
    override val removePresetTitle = "Fjerne favoritt?"
    override val remove = "Fjern"
    override val cancel = "Avbryt"
    override val save = "Lagre"
    override val presetActionFailed = "Det gikk ikke. Prøv igjen."
    override val addToPresets = "Legg til i favoritter"
    override val searchStations = "Søk etter radiokanaler"
    override val localRadio = "Lokal radio"
    override val browseAllCategories = "Bla gjennom alle kategorier"
    override val noResults = "Fant ingenting"
    override val playFailed = "Kunne ikke starte kanalen"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio og Spotify bruker receiverens BluOS-inngang. Radio fortsetter med kanalen du hørte på sist."
    override val chooseStation = "Velg en kanal"
    override val radioFavorites = "Radiofavoritter"
    override val radioReorderHint = "Hold inne og dra for å endre rekkefølgen."
    override val advancedSettings = "Avansert"
    override val autoConnect = "Auto-tilkobling"
    override val autoConnectDesc = "Koble til automatisk nÃ¥r appen Ã¥pnes"
    override val manualIp = "Manuell IP"
    override val connectedTo = "Koblet til"
    override val tapToConnect = "Trykk for Ã¥ finne din NAD-mottaker"
    
    override val favoriteSources = "Favorittinnganger"
    override val favoriteSourcesDesc = "Velg opptil 4 innganger for startskjermen"
    override val selectUpTo4 = "Velg opptil 4"
    override val noSourcesAvailable = "Koble til enheten fÃ¸rst"
    
    override val nowPlaying = "Spiller nÃ¥"
    override val nothingPlaying = "Ingenting spilles"
    
    override val presets = "ForhÃ¥ndsinnstillinger"
    override val noPresets = "Ingen lagrede forhÃ¥ndsinnstillinger"
    override val favoritePresetsDesc = "Velg opptil 3 forhÃ¥ndsinnstillinger for startskjermen"
    override val selectUpTo3 = "Velg opptil 3"
    
    override val services = "TJENESTER"
    
    override val power = "STRÃ˜M"
    override val input = "INNGANG"
    override val volume = "VOLUM"
    override val muted = "DEMPET"
    
    override val connected = "Tilkoblet"
    override val connecting = "Kobler til..."
    override val disconnected = "Frakoblet"
    override val error = "Feil"
    override val notConnected = "Ikke tilkoblet"
    override val openSettings = "Innstillinger"
    override val tryAgain = "PrÃ¸v igjen"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// DANISH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object DanishStrings : StringResources {
    override val appName = "NAD Fjernbetjening"
    
    override val settings = "Indstillinger"
    override val language = "Sprog"
    override val theme = "Tema"
    override val themeSystem = "System"
    override val themeLight = "Lyst"
    override val themeDark = "MÃ¸rkt"
    override val deviceConnection = "Enhed"
    override val deviceIp = "Enhedens IP-adresse"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Forbind"
    override val disconnect = "Afbryd"
    override val discoveredDevices = "Fundne enheder"
    override val scanNetwork = "SÃ¸g"
    override val scanning = "SÃ¸ger..."
    override val noDevicesFound = "Ingen enheder fundet. Tryk pÃ¥ SÃ¸g."
    override val clearSavedDevice = "Glem enhed"
    override val findNad = "Søg efter NAD"
    override val searchingNad = "Søger efter NAD…"
    override val nadNotFound = "NAD blev ikke fundet"
    override val sameWifiHint = "Telefonen og NAD skal være på samme Wi-Fi-netværk."
    override val enterIpManually = "Indtast IP manuelt"
    override val reorderHint = "Hold fingeren på ikonerne og træk. De første 3 vises på hovedskærmen."
    override val spotifyNoAudio = "Spotify får ingen lyd. Vælg kort “Denne telefon” i Spotify og derefter NAD igen."
    override val openSpotifyApp = "Åbn Spotify"
    override val presetEdit = "Rediger"
    override val presetDone = "Færdig"
    override val presetEditHint = "Tryk på en favorit for at omdøbe den. ✕ fjerner den."
    override val addCurrentPreset = "Føj det, der spiller, til favoritter"
    override val renamePreset = "Omdøb"
    override val removePresetTitle = "Fjern favorit?"
    override val remove = "Fjern"
    override val cancel = "Annuller"
    override val save = "Gem"
    override val presetActionFailed = "Det lykkedes ikke. Prøv igen."
    override val addToPresets = "Føj til favoritter"
    override val searchStations = "Søg efter radiostationer"
    override val localRadio = "Lokal radio"
    override val browseAllCategories = "Gennemse alle kategorier"
    override val noResults = "Intet fundet"
    override val playFailed = "Kunne ikke starte stationen"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio og Spotify bruger receiverens BluOS-indgang. Radio fortsætter med den station, du lyttede til sidst."
    override val chooseStation = "Vælg en station"
    override val radioFavorites = "Radiofavoritter"
    override val radioReorderHint = "Hold fingeren og træk for at ændre rækkefølgen."
    override val advancedSettings = "Avanceret"
    override val autoConnect = "Auto-forbindelse"
    override val autoConnectDesc = "Forbind automatisk nÃ¥r appen Ã¥bnes"
    override val manualIp = "Manuel IP"
    override val connectedTo = "Forbundet til"
    override val tapToConnect = "Tryk for at finde din NAD-modtager"
    
    override val favoriteSources = "Favoritindgange"
    override val favoriteSourcesDesc = "VÃ¦lg op til 4 indgange til startskÃ¦rmen"
    override val selectUpTo4 = "VÃ¦lg op til 4"
    override val noSourcesAvailable = "Forbind fÃ¸rst til enheden"
    
    override val nowPlaying = "Afspiller nu"
    override val nothingPlaying = "Intet afspilles"
    
    override val presets = "Forudindstillinger"
    override val noPresets = "Ingen gemte forudindstillinger"
    override val favoritePresetsDesc = "VÃ¦lg op til 3 forudindstillinger til startskÃ¦rmen"
    override val selectUpTo3 = "VÃ¦lg op til 3"
    
    override val services = "TJENESTER"
    
    override val power = "TÃ†ND/SLUK"
    override val input = "INDGANG"
    override val volume = "LYDSTYRKE"
    override val muted = "LYDLÃ˜S"
    
    override val connected = "Forbundet"
    override val connecting = "Forbinder..."
    override val disconnected = "Afbrudt"
    override val error = "Fejl"
    override val notConnected = "Ikke forbundet"
    override val openSettings = "Indstillinger"
    override val tryAgain = "PrÃ¸v igen"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// FRENCH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object FrenchStrings : StringResources {
    override val appName = "NAD TÃ©lÃ©commande"
    
    override val settings = "ParamÃ¨tres"
    override val language = "Langue"
    override val theme = "ThÃ¨me"
    override val themeSystem = "SystÃ¨me"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val deviceConnection = "Appareil"
    override val deviceIp = "Adresse IP de l'appareil"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Connecter"
    override val disconnect = "DÃ©connecter"
    override val discoveredDevices = "Appareils dÃ©tectÃ©s"
    override val scanNetwork = "Rechercher"
    override val scanning = "Recherche..."
    override val noDevicesFound = "Aucun appareil trouvÃ©. Appuyez sur Rechercher."
    override val clearSavedDevice = "Oublier l'appareil"
    override val findNad = "Rechercher le NAD"
    override val searchingNad = "Recherche du NAD…"
    override val nadNotFound = "NAD introuvable"
    override val sameWifiHint = "Le téléphone et le NAD doivent être sur le même réseau Wi-Fi."
    override val enterIpManually = "Saisir l'IP manuellement"
    override val reorderHint = "Appuyez longuement et faites glisser les icônes. Les 3 premières apparaissent sur l’écran principal."
    override val spotifyNoAudio = "Spotify ne reçoit pas de son. Dans Spotify, choisissez un instant « Ce téléphone », puis à nouveau le NAD."
    override val openSpotifyApp = "Ouvrir Spotify"
    override val presetEdit = "Modifier"
    override val presetDone = "Terminé"
    override val presetEditHint = "Touchez un favori pour le renommer. ✕ le supprime."
    override val addCurrentPreset = "Ajouter la lecture en cours aux favoris"
    override val renamePreset = "Renommer"
    override val removePresetTitle = "Supprimer le favori ?"
    override val remove = "Supprimer"
    override val cancel = "Annuler"
    override val save = "Enregistrer"
    override val presetActionFailed = "Échec. Veuillez réessayer."
    override val addToPresets = "Ajouter aux favoris"
    override val searchStations = "Rechercher une station"
    override val localRadio = "Radios locales"
    override val browseAllCategories = "Parcourir toutes les catégories"
    override val noResults = "Aucun résultat"
    override val playFailed = "Impossible de lancer cette station"
    override val radio = "Radio"
    override val virtualSourceHint = "La radio et Spotify utilisent l’entrée BluOS de l’ampli. La radio reprend la dernière station écoutée."
    override val chooseStation = "Choisissez une station"
    override val radioFavorites = "Radios favorites"
    override val radioReorderHint = "Appuyez longuement et faites glisser pour changer l’ordre."
    override val advancedSettings = "AvancÃ©"
    override val autoConnect = "Connexion auto"
    override val autoConnectDesc = "Se connecter automatiquement Ã  l'ouverture"
    override val manualIp = "IP manuelle"
    override val connectedTo = "ConnectÃ© Ã "
    override val tapToConnect = "Appuyez pour trouver votre rÃ©cepteur NAD"
    
    override val favoriteSources = "EntrÃ©es favorites"
    override val favoriteSourcesDesc = "Choisissez jusqu'Ã  4 entrÃ©es pour l'Ã©cran d'accueil"
    override val selectUpTo4 = "SÃ©lectionnez jusqu'Ã  4"
    override val noSourcesAvailable = "Connectez-vous d'abord Ã  l'appareil"
    
    override val nowPlaying = "En lecture"
    override val nothingPlaying = "Rien en lecture"
    
    override val presets = "PrÃ©rÃ©glages"
    override val noPresets = "Aucun prÃ©rÃ©glage enregistrÃ©"
    override val favoritePresetsDesc = "Choisissez jusqu'Ã  3 prÃ©rÃ©glages pour l'Ã©cran d'accueil"
    override val selectUpTo3 = "SÃ©lectionnez jusqu'Ã  3"
    
    override val services = "SERVICES"
    
    override val power = "MARCHE"
    override val input = "ENTRÃ‰E"
    override val volume = "VOLUME"
    override val muted = "MUET"
    
    override val connected = "ConnectÃ©"
    override val connecting = "Connexion..."
    override val disconnected = "DÃ©connectÃ©"
    override val error = "Erreur"
    override val notConnected = "Non connectÃ©"
    override val openSettings = "ParamÃ¨tres"
    override val tryAgain = "RÃ©essayer"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// ITALIAN
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object ItalianStrings : StringResources {
    override val appName = "NAD Telecomando"
    
    override val settings = "Impostazioni"
    override val language = "Lingua"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Chiaro"
    override val themeDark = "Scuro"
    override val deviceConnection = "Dispositivo"
    override val deviceIp = "Indirizzo IP dispositivo"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Connetti"
    override val disconnect = "Disconnetti"
    override val discoveredDevices = "Dispositivi trovati"
    override val scanNetwork = "Cerca"
    override val scanning = "Ricerca..."
    override val noDevicesFound = "Nessun dispositivo trovato. Tocca Cerca."
    override val clearSavedDevice = "Dimentica dispositivo"
    override val findNad = "Cerca NAD"
    override val searchingNad = "Ricerca NAD…"
    override val nadNotFound = "NAD non trovato"
    override val sameWifiHint = "Telefono e NAD devono essere sulla stessa rete Wi-Fi."
    override val enterIpManually = "Inserisci IP manualmente"
    override val reorderHint = "Tieni premuto e trascina le icone. Le prime 3 appaiono nella schermata principale."
    override val spotifyNoAudio = "Spotify non riceve audio. In Spotify scegli per un momento “Questo telefono”, poi di nuovo NAD."
    override val openSpotifyApp = "Apri Spotify"
    override val presetEdit = "Modifica"
    override val presetDone = "Fine"
    override val presetEditHint = "Tocca un preferito per rinominarlo. ✕ lo rimuove."
    override val addCurrentPreset = "Aggiungi il brano in riproduzione ai preferiti"
    override val renamePreset = "Rinomina"
    override val removePresetTitle = "Rimuovere il preferito?"
    override val remove = "Rimuovi"
    override val cancel = "Annulla"
    override val save = "Salva"
    override val presetActionFailed = "Non riuscito. Riprova."
    override val addToPresets = "Aggiungi ai preferiti"
    override val searchStations = "Cerca stazioni radio"
    override val localRadio = "Radio locali"
    override val browseAllCategories = "Sfoglia tutte le categorie"
    override val noResults = "Nessun risultato"
    override val playFailed = "Impossibile avviare la stazione"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio e Spotify usano l’ingresso BluOS del ricevitore. La radio riprende l’ultima stazione ascoltata."
    override val chooseStation = "Scegli una stazione"
    override val radioFavorites = "Radio preferite"
    override val radioReorderHint = "Tieni premuto e trascina per cambiare l’ordine."
    override val advancedSettings = "Avanzate"
    override val autoConnect = "Connessione auto"
    override val autoConnectDesc = "Connetti automaticamente all'apertura dell'app"
    override val manualIp = "IP manuale"
    override val connectedTo = "Connesso a"
    override val tapToConnect = "Tocca per trovare il tuo ricevitore NAD"
    
    override val favoriteSources = "Ingressi preferiti"
    override val favoriteSourcesDesc = "Scegli fino a 4 ingressi per la schermata iniziale"
    override val selectUpTo4 = "Seleziona fino a 4"
    override val noSourcesAvailable = "Prima connettiti al dispositivo"
    
    override val nowPlaying = "In riproduzione"
    override val nothingPlaying = "Nessuna riproduzione"
    
    override val presets = "Preimpostazioni"
    override val noPresets = "Nessuna preimpostazione salvata"
    override val favoritePresetsDesc = "Scegli fino a 3 preimpostazioni per la schermata iniziale"
    override val selectUpTo3 = "Seleziona fino a 3"
    
    override val services = "SERVIZI"
    
    override val power = "ACCENSIONE"
    override val input = "INGRESSO"
    override val volume = "VOLUME"
    override val muted = "MUTO"
    
    override val connected = "Connesso"
    override val connecting = "Connessione..."
    override val disconnected = "Disconnesso"
    override val error = "Errore"
    override val notConnected = "Non connesso"
    override val openSettings = "Impostazioni"
    override val tryAgain = "Riprova"
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SPANISH
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

object SpanishStrings : StringResources {
    override val appName = "NAD Control Remoto"
    
    override val settings = "Ajustes"
    override val language = "Idioma"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val deviceConnection = "Dispositivo"
    override val deviceIp = "DirecciÃ³n IP del dispositivo"
    override val deviceIpPlaceholder = "192.168.1.xxx"
    override val connect = "Conectar"
    override val disconnect = "Desconectar"
    override val discoveredDevices = "Dispositivos encontrados"
    override val scanNetwork = "Buscar"
    override val scanning = "Buscando..."
    override val noDevicesFound = "No se encontraron dispositivos. Toca Buscar."
    override val clearSavedDevice = "Olvidar dispositivo"
    override val findNad = "Buscar NAD"
    override val searchingNad = "Buscando NAD…"
    override val nadNotFound = "NAD no encontrado"
    override val sameWifiHint = "El teléfono y el NAD deben estar en la misma red Wi-Fi."
    override val enterIpManually = "Introducir IP manualmente"
    override val reorderHint = "Mantén pulsado y arrastra los iconos. Los 3 primeros aparecen en la pantalla principal."
    override val spotifyNoAudio = "Spotify no recibe audio. En Spotify, elige un momento «Este teléfono» y luego vuelve al NAD."
    override val openSpotifyApp = "Abrir Spotify"
    override val presetEdit = "Editar"
    override val presetDone = "Listo"
    override val presetEditHint = "Toca un favorito para cambiarle el nombre. ✕ lo elimina."
    override val addCurrentPreset = "Añadir lo que suena a favoritos"
    override val renamePreset = "Cambiar nombre"
    override val removePresetTitle = "¿Eliminar favorito?"
    override val remove = "Eliminar"
    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val presetActionFailed = "No se pudo. Inténtalo de nuevo."
    override val addToPresets = "Añadir a favoritos"
    override val searchStations = "Buscar emisoras"
    override val localRadio = "Radios locales"
    override val browseAllCategories = "Ver todas las categorías"
    override val noResults = "No se encontró nada"
    override val playFailed = "No se pudo iniciar la emisora"
    override val radio = "Radio"
    override val virtualSourceHint = "Radio y Spotify usan la entrada BluOS del receptor. La radio continúa con la última emisora escuchada."
    override val chooseStation = "Elige una emisora"
    override val radioFavorites = "Radios favoritas"
    override val radioReorderHint = "Mantén pulsado y arrastra para cambiar el orden."
    override val advancedSettings = "Avanzado"
    override val autoConnect = "ConexiÃ³n automÃ¡tica"
    override val autoConnectDesc = "Conectar automÃ¡ticamente al abrir la app"
    override val manualIp = "IP manual"
    override val connectedTo = "Conectado a"
    override val tapToConnect = "Toca para encontrar tu receptor NAD"
    
    override val favoriteSources = "Entradas favoritas"
    override val favoriteSourcesDesc = "Elige hasta 4 entradas para la pantalla de inicio"
    override val selectUpTo4 = "Selecciona hasta 4"
    override val noSourcesAvailable = "Primero conecta al dispositivo"
    
    override val nowPlaying = "Reproduciendo"
    override val nothingPlaying = "Nada en reproducciÃ³n"
    
    override val presets = "PresintonÃ­as"
    override val noPresets = "No hay presintonÃ­as guardadas"
    override val favoritePresetsDesc = "Elige hasta 3 presintonÃ­as para la pantalla de inicio"
    override val selectUpTo3 = "Selecciona hasta 3"
    
    override val services = "SERVICIOS"
    
    override val power = "ENCENDIDO"
    override val input = "ENTRADA"
    override val volume = "VOLUMEN"
    override val muted = "SILENCIADO"
    
    override val connected = "Conectado"
    override val connecting = "Conectando..."
    override val disconnected = "Desconectado"
    override val error = "Error"
    override val notConnected = "No conectado"
    override val openSettings = "Ajustes"
    override val tryAgain = "Reintentar"
}


