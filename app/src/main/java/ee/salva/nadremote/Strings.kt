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


