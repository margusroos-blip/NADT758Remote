package com.nadremote.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * NAD WebSocket Client - Port 8585
 */
class NadClient {

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null

    private val _state = MutableStateFlow(NadState())
    val state: StateFlow<NadState> = _state.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun connect(ip: String) {
        if (ip.isBlank()) return
        if (!DeviceAddressPolicy.isAllowedDeviceAddress(ip)) {
            _connectionStatus.value = ConnectionStatus.ERROR
            _error.value = "Only local network device addresses are allowed"
            return
        }
        disconnect()

        _connectionStatus.value = ConnectionStatus.CONNECTING
        _error.value = null

        val req = Request.Builder()
            .url("ws://$ip:8585/")
            .build()

        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _error.value = null
                queryAll()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseResponse(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                parseResponse(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ConnectionStatus.ERROR
                _error.value = t.message ?: "Ãœhenduse viga"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        })
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun send(cmd: String) {
        ws?.send(cmd)
    }

    private fun queryAll() {
        send("Main.Model?")
        send("Main.Power?")
        send("Main.Mute?")
        send("Main.Volume?")
        send("Main.Source?")
        // Source names - NAD T758 kasutab "Source1?" mitte "Main.Source1?"
        for (i in 1..10) {
            send("Source$i?")
        }
    }

    private fun parseResponse(payload: String) {
        payload.split("\r\n", "\n")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { line ->
                val idx = line.indexOf("=")
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                handleKeyValue(key, value)
            }
    }

    private fun handleKeyValue(key: String, value: String) {
        _state.update { current ->
            when (key) {
                "Main.Model" -> current.copy(model = value)
                
                "Main.Power" -> current.copy(
                    power = value.equals("On", true)
                )
                
                "Main.Mute" -> current.copy(
                    mute = value.equals("On", true)
                )
                
                "Main.Volume" -> current.copy(
                    volume = value.toIntOrNull() ?: current.volume
                )
                
                "Main.Source" -> current.copy(
                    sourceId = value.toIntOrNull() ?: current.sourceId
                )
                
                else -> {
                    // Source names: "Source1.Name=Telia TV"
                    val sourceNameMatch = Regex("^Source(\\d+)\\.Name$").find(key)
                    if (sourceNameMatch != null) {
                        val id = sourceNameMatch.groupValues[1].toIntOrNull()
                        if (id != null && value.isNotBlank()) {
                            current.copy(sources = current.sources + (id to value))
                        } else current
                    } else {
                        // Source enabled: "Source1.Enabled=Yes"
                        val sourceEnabledMatch = Regex("^Source(\\d+)\\.Enabled$").find(key)
                        if (sourceEnabledMatch != null) {
                            val id = sourceEnabledMatch.groupValues[1].toIntOrNull()
                            if (id != null) {
                                val enabled = value.equals("Yes", true)
                                current.copy(sourcesEnabled = current.sourcesEnabled + (id to enabled))
                            } else current
                        } else current
                    }
                }
            }
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // Commands
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    fun powerOn() {
        send("Main.Power=On")
        send("Main.Power?")
    }
    
    fun powerOff() {
        send("Main.Power=Off")
        send("Main.Power?")
    }
    
    fun powerToggle() {
        // Check current state and toggle
        if (_state.value.power) {
            powerOff()
        } else {
            powerOn()
        }
    }

    fun mute() {
        send("Main.Mute=On")
        send("Main.Mute?")
    }
    
    fun unmute() {
        send("Main.Mute=Off")
        send("Main.Mute?")
    }
    
    fun muteToggle() {
        if (_state.value.mute) {
            unmute()
        } else {
            mute()
        }
    }

    fun volumeUp() {
        send("Main.Volume+")
        send("Main.Volume?")
    }
    
    fun volumeDown() {
        send("Main.Volume-")
        send("Main.Volume?")
    }
    
    fun setVolume(db: Int) {
        send("Main.Volume=$db")
        send("Main.Volume?")
    }

    fun setSource(id: Int) {
        send("Main.Source=$id")
        send("Main.Source?")
    }

    fun refresh() = queryAll()
}


