package com.nadremote.app

object DeviceAddressPolicy {

    fun isAllowedDeviceAddress(address: String): Boolean {
        val normalized = address.trim()
        if (normalized.isBlank()) return false
        if (normalized.endsWith(".local", ignoreCase = true)) return true

        val parts = normalized.split('.')
        if (parts.size != 4) return false

        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false

        val a = octets[0]
        val b = octets[1]
        val c = octets[2]
        val d = octets[3]

        if (a == 10) return true
        if (a == 172 && b in 16..31) return true
        if (a == 192 && b == 168) return true
        if (a == 169 && b == 254) return true
        if (a == 127) return true
        if (a == 0 && b == 0 && c == 0 && d == 0) return false

        return false
    }
}


