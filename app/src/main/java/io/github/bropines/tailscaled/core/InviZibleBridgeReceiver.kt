package io.github.bropines.tailscaled.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * InviZible Pro Max bridge — status channel only.
 *
 * Start/stop are driven by InviZible starting the exported [TailscaledService] directly from its
 * foreground context (a backgrounded app cannot start its own foreground service on Android 12+,
 * so a broadcast-receiver-initiated start is not viable). This object only broadcasts service
 * status back to InviZible.
 */
object InviZibleBridgeReceiver {

    private const val TAG = "InviZibleBridge"

    const val ACTION_STATUS = "io.github.bropines.tailscaled.bridge.STATUS"
    const val EXTRA_RUNNING = "running"

    // Set true on the START intent by InviZible Pro Max to force SOCKS-only operation (no TUN VPN),
    // so TailSocks never grabs the system VpnService slot InviZible owns. See audit HIGH #3.
    const val EXTRA_BRIDGE_SOCKS_ONLY = "bridge_socks_only"

    // Gated by the signature-level BRIDGE_PERMISSION rather than a fixed target package:
    // InviZible's flavors carry suffixes (.stable/.beta/etc), so we can't hardcode the receiver
    // package. Only a same-signature app holding the permission (our InviZible Pro Max) receives it.
    private const val BRIDGE_PERMISSION = "pan.alexander.tordnscrypt.BRIDGE_PERMISSION"

    /**
     * Send status to InviZible, addressed explicitly to co-signed receivers only.
     *
     * SECURITY (audit MEDIUM #12): this used to be an implicit broadcast carrying only the
     * permission name. Android resolves a custom permission to whichever package declared it
     * first, so an app installed before both of ours can declare BRIDGE_PERMISSION at
     * protectionLevel="normal", be granted it, and receive every status broadcast we send.
     *
     * Hardcoding a target package is still not an option — InviZible's flavors carry suffixes —
     * so resolve the receivers at runtime and keep only those signed with our key, then address
     * the broadcast to each explicitly. The permission is retained as a second layer.
     */
    fun broadcastStatus(context: Context, running: Boolean) {
        val intent = Intent(ACTION_STATUS).apply { putExtra(EXTRA_RUNNING, running) }
        try {
            val pm = context.packageManager
            val self = context.packageName
            val targets = pm.queryBroadcastReceivers(intent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .distinct()
                .filter { pm.checkSignatures(self, it) == PackageManager.SIGNATURE_MATCH }

            if (targets.isEmpty()) {
                Log.w(TAG, "Bridge status not sent: no co-signed receiver found")
                return
            }
            targets.forEach { pkg ->
                context.sendBroadcast(Intent(intent).setPackage(pkg), BRIDGE_PERMISSION)
            }
        } catch (e: Exception) {
            // Fail closed: not broadcasting costs InviZible a start-timeout and a config rollback,
            // which is recoverable. Broadcasting blind to an unverified peer is not.
            Log.e(TAG, "Bridge status not sent", e)
        }
    }
}
