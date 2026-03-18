package com.overcomevpn.core

import android.util.Base64
import org.json.JSONObject

data class VpnConfig(
    val protocol: String,
    val address: String,
    val port: Int,
    val uuid: String = "",
    val password: String = "",
    val remark: String = "Server"
)

object ConfigParser {
    fun parse(raw: String): VpnConfig? {
        val key = raw.trim()
        return when {
            key.startsWith("vless://")  -> parseVless(key)
            key.startsWith("vmess://")  -> parseVmess(key)
            key.startsWith("ss://")     -> parseShadowsocks(key)
            key.startsWith("trojan://") -> parseTrojan(key)
            else -> null
        }
    }

    private fun parseVless(key: String): VpnConfig? = try {
        val uri = java.net.URI(key)
        VpnConfig(
            protocol = "vless",
            address  = uri.host,
            port     = uri.port,
            uuid     = uri.userInfo ?: "",
            remark   = java.net.URLDecoder.decode(uri.fragment ?: "Server", "UTF-8")
        )
    } catch (e: Exception) { null }

    private fun parseVmess(key: String): VpnConfig? = try {
        val b64 = key.removePrefix("vmess://")
        val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
        VpnConfig(
            protocol = "vmess",
            address  = json.getString("add"),
            port     = json.getString("port").toInt(),
            uuid     = json.getString("id"),
            remark   = json.optString("ps", "VMess Server")
        )
    } catch (e: Exception) { null }

    private fun parseShadowsocks(key: String): VpnConfig? = try {
        var s = key.removePrefix("ss://")
        val remark = if (s.contains("#")) {
            val r = s.substringAfter("#")
            s = s.substringBefore("#")
            java.net.URLDecoder.decode(r, "UTF-8")
        } else "SS Server"
        val atIdx = s.lastIndexOf("@")
        val userInfo = String(Base64.decode(s.substring(0, atIdx), Base64.DEFAULT))
        val hostPort = s.substring(atIdx + 1)
        VpnConfig(
            protocol = "shadowsocks",
            address  = hostPort.substringBeforeLast(":"),
            port     = hostPort.substringAfterLast(":").toInt(),
            password = userInfo,
            remark   = remark
        )
    } catch (e: Exception) { null }

    private fun parseTrojan(key: String): VpnConfig? = try {
        val uri = java.net.URI(key)
        VpnConfig(
            protocol = "trojan",
            address  = uri.host,
            port     = uri.port,
            password = uri.userInfo ?: "",
            remark   = java.net.URLDecoder.decode(uri.fragment ?: "Trojan", "UTF-8")
        )
    } catch (e: Exception) { null }
}
