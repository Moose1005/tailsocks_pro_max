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

    /**
     * Set on the START intent by InviZible Pro Max to identify itself, so status can be addressed
     * to it explicitly (audit #12). Its signature is verified before it is trusted, so a forged
     * value gains nothing.
     */
    const val EXTRA_BRIDGE_CLIENT_PACKAGE = "bridge_client_package"

    // Gated by the signature-level BRIDGE_PERMISSION rather than a fixed target package:
    // InviZible's flavors carry suffixes (.stable/.beta/etc), so we can't hardcode the receiver
    // package. Only a same-signature app holding the permission (our InviZible Pro Max) receives it.
    private const val BRIDGE_PERMISSION = "pan.alexander.tordnscrypt.BRIDGE_PERMISSION"

    /** Package InviZible identified itself as on the last bridge START, once signature-verified. */
    @Volatile
    private var verifiedClientPackage: String? = null

    /**
     * Record the package InviZible claims on a bridge START, but only after confirming it is signed
     * with our key. Anything unverifiable is ignored and we fall back to the permission-gated
     * broadcast.
     */
    fun rememberClient(context: Context, claimedPackage: String?) {
        if (claimedPackage.isNullOrBlank()) return
        verifiedClientPackage = try {
            if (context.packageManager.checkSignatures(context.packageName, claimedPackage)
                == PackageManager.SIGNATURE_MATCH
            ) {
                Log.d(TAG, "Bridge client verified: $claimedPackage")
                claimedPackage
            } else {
                Log.w(TAG, "Bridge client $claimedPackage is NOT co-signed — ignoring the claim")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify bridge client $claimedPackage", e)
            null
        }
    }

    /**
     * Send status to InviZible.
     *
     * SECURITY (audit MEDIUM #12): this was an implicit broadcast carrying only the permission
     * name. Android resolves a custom permission to whichever package declared it first, so an app
     * installed before both of ours can declare BRIDGE_PERMISSION at protectionLevel="normal", be
     * granted it, and receive every status broadcast we send.
     *
     * Where InviZible has identified itself on a bridge START and that package verified as
     * co-signed, the broadcast is addressed to it explicitly and the squatter is cut out entirely.
     *
     * Otherwise we fall back to the permission-gated implicit broadcast. That is the weaker path,
     * but it is the one that must not be removed: InviZible registers its bridge receivers **at
     * runtime**, so `PackageManager.queryBroadcastReceivers` cannot see them, and an earlier
     * version of this method that resolved receivers that way found none and refused to send —
     * which silently broke the bridge entirely and stalled every start until it timed out.
     * Not delivering status is not a safe failure here; it is a broken product.
     */
    fun broadcastStatus(context: Context, running: Boolean) {
        val intent = Intent(ACTION_STATUS).apply { putExtra(EXTRA_RUNNING, running) }
        try {
            val target = verifiedClientPackage
            if (target != null) {
                context.sendBroadcast(Intent(intent).setPackage(target), BRIDGE_PERMISSION)
            } else {
                context.sendBroadcast(intent, BRIDGE_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bridge status not sent", e)
        }
    }
}
