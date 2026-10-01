package com.nadremote.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Device Discovery via mDNS and subnet scan
 */
class NadDiscovery(private val context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var resolveChannel: Channel<NsdServiceInfo>? = null
    private var resolveJob: Job? = null

    private val found = ConcurrentHashMap<String, FoundDevice>()

    private val _devices = MutableStateFlow<List<FoundDevice>>(emptyList())
    val devices: StateFlow<List<FoundDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // mDNS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun startMdns() {
        stopMdns()

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wm.createMulticastLock("nad-mdns").apply {
            setReferenceCounted(true)
            acquire()
        }

        resolveChannel = Channel(Channel.UNLIMITED)
        resolveJob = scope.launch {
            val ch = resolveChannel ?: return@launch
            for (serviceInfo in ch) {
                resolveServiceSequentially(serviceInfo)
                delay(100)
            }
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolveChannel?.trySend(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { stopMdns() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsd.discoverServices("_musc._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (_: Exception) {}
    }

    fun stopMdns() {
        discoveryListener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discoveryListener = null
        resolveJob?.cancel()
        resolveJob = null
        resolveChannel?.close()
        resolveChannel = null
        multicastLock?.let { runCatching { it.release() } }
        multicastLock = null
    }

    fun close() {
        stopMdns()
        scope.cancel()
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Subnet Scan
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    suspend fun scanSubnet(): List<FoundDevice> = coroutineScope {
        _isScanning.value = true

        val hosts = scanTargets()
        if (hosts.isEmpty()) {
            _isScanning.value = false
            return@coroutineScope emptyList()
        }

        val results = mutableListOf<FoundDevice>()
        val semaphore = Semaphore(50)

        hosts.map { ip ->
            async(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    if (isPortOpen(ip, 8585, 200)) {
                        val device = identify(ip)
                        synchronized(results) { results.add(device) }
                        found[ip] = device
                        updateList()
                    }
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()

        _isScanning.value = false
        results
    }

    /**
     * Wi-Fi võrgu aadressid, mida skaneerida. Arvestab päris võrgumaski (nt /22 võrgus
     * 192.168.68.0-192.168.71.255), mitte ainult telefoni enda /24 vahemikku.
     * Suurema võrgu korral piirdub telefoni ümbritseva /22 plokiga (max 1022 aadressi).
     * Telefoni enda /24 tuleb esimesena, sest seade on enamasti seal.
     */
    private fun scanTargets(): List<String> {
        val (ownIp, prefix) = wifiAddress() ?: return emptyList()
        val effectivePrefix = prefix.coerceIn(MIN_SCAN_PREFIX, 30)
        val mask = (-1 shl (32 - effectivePrefix))
        val network = ownIp and mask
        val broadcast = network or mask.inv()

        val ownBlock = ownIp and (-1 shl 8)
        return ((network + 1) until broadcast)
            .filter { it != ownIp }
            .sortedBy { if ((it and (-1 shl 8)) == ownBlock) 0 else 1 }
            .map { intToIp(it) }
    }

    /** Telefoni IPv4 aadress (big-endian Int) ja prefiksi pikkus Wi-Fi võrgus. */
    private fun wifiAddress(): Pair<Int, Int>? {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            @Suppress("DEPRECATION")
            val networks = listOfNotNull(cm.activeNetwork) + cm.allNetworks.toList()
            for (network in networks.distinct()) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) continue
                val link = cm.getLinkProperties(network)?.linkAddresses
                    ?.firstOrNull { it.address is Inet4Address } ?: continue
                val bytes = link.address.address
                val ip = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
                return ip to link.prefixLength
            }
        }

        // Varuvariant: vana WifiManager API, võrgumask teadmata -> /24
        @Suppress("DEPRECATION")
        val dhcp = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.dhcpInfo
        if (dhcp == null || dhcp.ipAddress == 0) return null
        val ip = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dhcp.ipAddress)
            .order(ByteOrder.BIG_ENDIAN).getInt(0)
        return ip to 24
    }

    private fun intToIp(ip: Int): String =
        "${(ip ushr 24) and 0xFF}.${(ip ushr 16) and 0xFF}.${(ip ushr 8) and 0xFF}.${ip and 0xFF}"

    private companion object {
        // Kõige laiem võrk, mida täies ulatuses skaneerime (/22 = 1022 aadressi)
        const val MIN_SCAN_PREFIX = 22
    }

    /**
     * Loeb BluOS-ist (port 11000 /SyncStatus) seadme nime ja MAC-aadressi, et kasutajale
     * näidata nime ("Elutoa ressiiver NAD T758"), mitte IP-d.
     * Kui BluOS ei vasta, tagastab "NAD" ilma MAC-ita.
     */
    fun identify(ip: String): FoundDevice {
        return try {
            val conn = URL("http://$ip:11000/SyncStatus").openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            val xml = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val tag = xml.substringAfter("<SyncStatus", "").substringBefore(">")
            fun attr(name: String) = Regex("""\b$name="([^"]*)"""").find(tag)?.groupValues?.get(1).orEmpty()
                .replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            FoundDevice(
                name = attr("name").ifBlank { attr("modelName").ifBlank { "NAD" } },
                ip = ip,
                mac = attr("mac").uppercase()
            )
        } catch (_: Exception) {
            FoundDevice("NAD", ip)
        }
    }

    private fun isPortOpen(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use {
                it.connect(InetSocketAddress(host, port), timeout)
                true
            }
        } catch (_: Exception) { false }
    }

    fun clear() {
        found.clear()
        updateList()
    }

    private fun updateList() {
        _devices.value = found.values.distinctBy { it.ip }.sortedBy { it.ip }
    }

    private class Semaphore(max: Int) {
        private val ch = Channel<Unit>(max).apply { repeat(max) { trySend(Unit) } }
        suspend fun acquire() = ch.receive()
        fun release() { ch.trySend(Unit) }
    }

    private suspend fun resolveServiceSequentially(serviceInfo: NsdServiceInfo) {
        val device = withTimeoutOrNull(2500) {
            suspendCancellableCoroutine<FoundDevice?> { cont ->
                try {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            if (cont.isActive) cont.resume(null)
                        }

                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val ip = resolved.host?.hostAddress
                            if (ip.isNullOrBlank()) {
                                if (cont.isActive) cont.resume(null)
                                return
                            }
                            val name = resolved.serviceName ?: "NAD"
                            if (cont.isActive) cont.resume(FoundDevice(name, ip))
                        }
                    })
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

        // mDNS (_musc) leiab kõik BluOS-i mängijad; näita ainult neid, millel on NAD-i juhtport
        device?.let {
            if (!isPortOpen(it.ip, 8585, 500)) return
            val identified = identify(it.ip)
            found[it.ip] = identified.copy(name = identified.name.ifBlank { it.name })
            updateList()
        }
    }
}


