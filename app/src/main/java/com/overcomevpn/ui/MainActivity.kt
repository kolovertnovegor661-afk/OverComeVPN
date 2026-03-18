package com.overcomevpn.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.overcomevpn.R
import com.overcomevpn.core.ConfigParser
import com.overcomevpn.core.OvercomeVpnService
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var btnConnect: Button
    private lateinit var etKey: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvProtocol: TextView
    private lateinit var btnPaste: ImageButton

    private val VPN_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConnect = findViewById(R.id.btnConnect)
        etKey = findViewById(R.id.etKey)
        tvStatus = findViewById(R.id.tvStatus)
        tvProtocol = findViewById(R.id.tvProtocol)
        btnPaste = findViewById(R.id.btnPaste)

        btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (text != null) {
                etKey.setText(text)
                detectProtocol(text)
                Toast.makeText(this, "Ключ вставлен", Toast.LENGTH_SHORT).show()
            }
        }

        etKey.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                detectProtocol(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnConnect.setOnClickListener {
            if (OvercomeVpnService.isRunning) disconnect() else connect()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun detectProtocol(key: String) {
        val protocol = when {
            key.startsWith("vless://")  -> "VLESS"
            key.startsWith("vmess://")  -> "VMess"
            key.startsWith("ss://")     -> "Shadowsocks"
            key.startsWith("trojan://") -> "Trojan"
            else -> "—"
        }
        tvProtocol.text = "Протокол: $protocol"
    }

    private fun connect() {
        val key = etKey.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, "Вставьте VPN ключ", Toast.LENGTH_SHORT).show()
            return
        }
        val config = ConfigParser.parse(key)
        if (config == null) {
            Toast.makeText(this, "Неверный формат ключа", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, VPN_REQUEST_CODE)
        else startVpnService(key)
    }

    private fun startVpnService(key: String) {
        val config = ConfigParser.parse(key) ?: return
        val v2rayConfig = buildV2RayConfig(config)

        val intent = Intent(this, OvercomeVpnService::class.java)
        intent.putExtra("vpn_config", v2rayConfig)
        startService(intent)
        updateUI()
    }

    private fun buildV2RayConfig(config: com.overcomevpn.core.VpnConfig): String {
        return JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("inbounds", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("port", 10808)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                })
            })
            put("outbounds", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("protocol", config.protocol)
                    put("settings", JSONObject().apply {
                        put("vnext", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("address", config.address)
                                put("port", config.port)
                                put("users", org.json.JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("id", config.uuid)
                                        put("encryption", "none")
                                    })
                                })
                            })
                        })
                    })
                    put("streamSettings", JSONObject().apply {
                        put("network", config.network)
                        put("security", config.tls)
                    })
                })
            })
        }.toString()
    }

    private fun disconnect() {
        val intent = Intent(this, OvercomeVpnService::class.java)
        stopService(intent)
        updateUI()
        Toast.makeText(this, "Отключено", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        if (OvercomeVpnService.isRunning) {
            btnConnect.text = "ОТКЛЮЧИТЬ"
            btnConnect.backgroundTintList = androidx.core.content.res.ResourcesCompat
                .getColorStateList(resources, R.color.red, null)
            tvStatus.text = "● Подключено"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            btnConnect.text = "ПОДКЛЮЧИТЬ"
            btnConnect.backgroundTintList = androidx.core.content.res.ResourcesCompat
                .getColorStateList(resources, R.color.blue, null)
            tvStatus.text = "● Отключено"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpnService(etKey.text.toString().trim())
        }
    }
}
