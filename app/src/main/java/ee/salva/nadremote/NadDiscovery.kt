package com.nadremote.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket
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

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val dhcp = wm?.dhcpInfo
        if (dhcp == null || dhcp.ipAddress == 0) {
            _isScanning.value = false
            return@coroutineScope emptyList()
        }

        val ipBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dhcp.ipAddress).array()
        val baseIp = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}"

        val results = mutableListOf<FoundDevice>()
        val semaphore = Semaphore(50)

        (1..254).map { i ->
            async(Dispatchers.IO) {
                semaphore.acquire()
                try {
                    val ip = "$baseIp.$i"
                    if (isPortOpen(ip, 8585, 200)) {
                        val device = FoundDevice("NAD @ $ip", ip)
                        synchronized(results) { results.add(device) }
                        found["scan@$ip"] = device
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

        device?.let {
            found["${it.name}@${it.ip}"] = it
            updateList()
        }
    }
}


