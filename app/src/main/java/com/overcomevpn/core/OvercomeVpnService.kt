package com.overcomevpn.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.v2ray.ang.util.V2rayConfigUtil
import go.Seq
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet

class OvercomeVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var v2rayPoint: V2RayPoint? = null

    companion object {
        const val CHANNEL_ID = "overcomevpn_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        Seq.setContext(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = intent?.getStringExtra("vpn_config") ?: return START_NOT_STICKY
        startV2Ray(config)
        return START_STICKY
    }

    private fun startV2Ray(config: String) {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())

            v2rayPoint = Libv2ray.newV2RayPoint(object : V2RayVPNServiceSupportsSet {
                override fun shutdown(): Long {
                    stopSelf()
                    return 0
                }
                override fun prepare(): Long = 0
                override fun protect(l: Long): Boolean = protect(l.toInt())
                override fun onEmitStatus(l: Long, s: String?): Long = 0
                override fun setup(s: String): Long {
                    try {
                        val builder = Builder()
                            .setSession("OvercomeVPN")
                            .addAddress("10.0.0.2", 24)
                            .addDnsServer("8.8.8.8")
                            .addDnsServer("1.1.1.1")
                            .addRoute("0.0.0.0", 0)
                            .setMtu(1500)
                        vpnInterface = builder.establish()
                        return vpnInterface?.fd?.toLong() ?: -1
                    } catch (e: Exception) {
                        return -1
                    }
                }
            }, false)

            v2rayPoint?.configureFileContent = config
            v2rayPoint?.runLoop(false)
            isRunning = true

        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "OvercomeVPN", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("OvercomeVPN")
                .setContentText("Подключено")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("OvercomeVPN")
                .setContentText("Подключено")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }
    }

    override fun onDestroy() {
        v2rayPoint?.stopLoop()
        vpnInterface?.close()
        isRunning = false
        super.onDestroy()
    }
}
