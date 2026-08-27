package io.github.bropines.tailscaled.core

import android.content.Context
import android.content.Intent

/**
 * InviZible Pro Max bridge — status channel only.
 *
 * Start/stop are driven by InviZible starting the exported [TailscaledService] directly from its
 * foreground context (a backgrounded app cannot start its own foreground service on Android 12+,
 * so a broadcast-receiver-initiated start is not viable). This object only broadcasts service
 * status back to InviZible.
 */
object InviZibleBridgeReceiver {

    const val ACTION_STATUS = "io.github.bropines.tailscaled.bridge.STATUS"
    const val EXTRA_RUNNING = "running"

    // Set true on the START intent by InviZible Pro Max to force SOCKS-only operation (no TUN VPN),
    // so TailSocks never grabs the system VpnService slot InviZible owns. See audit HIGH #3.
    const val EXTRA_BRIDGE_SOCKS_ONLY = "bridge_socks_only"

    // Gated by the signature-level BRIDGE_PERMISSION rather than a fixed target package:
    // InviZible's flavors carry suffixes (.stable/.beta/etc), so we can't hardcode the receiver
    // package. Only a same-signature app holding the permission (our InviZible Pro Max) receives it.
    private const val BRIDGE_PERMISSION = "pan.alexander.tordnscrypt.BRIDGE_PERMISSION"

    fun broadcastStatus(context: Context, running: Boolean) {
        context.sendBroadcast(
            Intent(ACTION_STATUS).apply {
                putExtra(EXTRA_RUNNING, running)
            },
            BRIDGE_PERMISSION
        )
    }
}
