package com.overcomevpn.core

import android.net.VpnService
import android.content.Intent
import android.os.ParcelFileDescriptor

class OvercomeVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        super.onDestroy()
    }
}
