# Using TailSocks with InviZible Pro

This guide explains how to run TailSocks alongside InviZible Pro on an unrooted Android device.
InviZible holds Android's single VPN slot (DNSCrypt + optional Tor/I2P). TailSocks runs entirely
in userspace — no `VpnService` required — and exposes a local SOCKS5 proxy that InviZible can
route selected apps through.

**Result:** DNSCrypt protects all DNS, Tor protects apps you assign to it, and chosen apps
reach your Tailnet over an encrypted WireGuard tunnel — simultaneously, without root.

> **TUN mode warning:** TailSocks' optional TUN VPN mode calls `VpnService` and will conflict
> with InviZible. Keep **TUN Mode** disabled (it is off by default). Check under
> TailSocks → Settings → Network → TUN Mode.

---

## Building this fork from source

This repository is **Moose1005/tailsocks_pro_max**, a fork of
[bropines/tailsocks](https://github.com/bropines/tailsocks) carrying the InviZible Pro Max bridge.

**Clone with submodules — this is not optional:**

```
git clone --recurse-submodules https://github.com/Moose1005/tailsocks_pro_max.git
```

If you already cloned without them:

```
git submodule update --init --recursive
```

Skipping this produces an APK that builds and installs cleanly but has **no TUN mode at all**.
`app/build.gradle.kts` only wires up `ndkBuild` when the prebuilt library is missing, and
`ndkBuild` needs the `app/src/main/jni/hev-socks5-tunnel` sources. With an empty submodule
directory it compiles nothing and reports no error, and at runtime `TunVpnService.nativeLoaded`
is false, so both `startTunMode()` and `stopTunMode()` return immediately.

The symptom is misleading: the UI can still display **"Active + TUN"**, because that label
reflects the *setting* rather than an established tunnel. Verify with:

```
unzip -l app/build/outputs/apk/debug/app-x86_64-debug.apk | grep hev-socks5-tunnel
```

Nothing listed means TUN is not present in that build.

The Go core (`appctr.aar`) is built separately by the scripts in `appctr/`. On Windows use
`build_x86_64_win.sh` or `build_all_win.sh` - upstream's `build.sh` has Linux-only NDK paths.
**Check the result is a valid archive before building the app**, since `gomobile bind` has been
observed exiting successfully while leaving a truncated file, which Gradle only reports much later
as an unrelated-looking `invalid block type`:

```
unzip -t appctr/tmp/appctr.aar
```

---

## Prerequisites

- **InviZible Pro** — use the [GitHub/F-Droid build](https://github.com/Moose1005/InviZible-Pro-Max), not
  Play Store (the Play version drops some DNSCrypt options).
- **TailSocks** — download the latest APK from
  [GitHub Releases](https://github.com/bropines/tailsocks/releases/latest). Android will warn
  about an unknown source; that is expected for a sideloaded APK.
- A Tailscale account with at least one other node in your tailnet.

---

## Step 1 — Set up TailSocks

1. Install and open TailSocks. Sign in to your tailnet (an auth key from
   [tailscale.com/admin](https://login.tailscale.com/admin/authkeys) is fastest).
2. Go to **Settings → Network** and confirm:
   - **TUN Mode** is **off**.
   - **SOCKS5 Proxy Address** is `127.0.0.1:48115` (the default; note the port if you change it).
   - **DNS Proxy Address** is `127.0.0.1:1053` (the default; note the port if you change it).
3. Start the TailSocks service. Wait for the status to show **Connected**.

---

## Step 2 — Configure InviZible's SOCKS5 proxy

1. Open InviZible Pro → **Settings** (gear icon) → **Common settings** → **Proxy**.
2. Set **Proxy server** to `127.0.0.1`.
3. Set **Proxy port** to `48115` (or whatever you noted in Step 1).
4. Leave **Username** and **Password** blank unless you configured SOCKS5 auth in TailSocks.
5. Tap **Select apps that bypass proxy** and add the apps you want routed through the Tailnet.
   - Start with one app (e.g. a browser or SSH client) to verify before adding more.
   - Do **not** add TailSocks itself here — see Step 3.
6. Enable **Use proxy for selected apps**.
7. Tap **Save**.

> The "DNSCrypt outbound proxy" and "Tor outbound proxy" checkboxes route the *modules themselves*
> through TailSocks — useful if InviZible's coordination traffic is blocked in your region, but not
> needed for the basic setup.

---

## Step 3 — Exclude TailSocks from InviZible's VPN tunnel

TailSocks needs a direct path to Tailscale's coordination server and DERP relays. If its own
traffic is captured by InviZible's tunnel, the connection loop will prevent it from authenticating.

1. In InviZible Pro → **Settings** → **Firewall** (or **Apps**), find the per-app routing list.
2. Locate **TailSocks** (`io.github.bropines.tailscaled`) and set it to **Bypass VPN** (direct
   internet, no proxy, no Tor).

---

## Step 4 — Enable MagicDNS for `.ts.net` names (optional but recommended)

TailSocks' built-in DNS proxy resolves MagicDNS names (e.g. `my-laptop.ts.net`) at zero latency
from an in-memory node cache — no round-trip required. To make these names work while DNSCrypt
owns the device's DNS:

1. In InviZible Pro → **Settings** → **DNSCrypt** → **DNS rules** → **Forwarding rules**.
2. Add the following line and save:
   ```
   ts.net 127.0.0.1:1053
   ```
   This tells DNSCrypt-proxy to forward all `.ts.net` queries to TailSocks' local DNS proxy
   instead of resolving them upstream.
3. If you use split-DNS domains configured in Tailscale (e.g. `corp.example.com`), add those too:
   ```
   ts.net 127.0.0.1:1053
   corp.example.com 127.0.0.1:1053
   ```

> TailSocks must be running and connected before DNSCrypt can reach port 1053. If TailSocks is
> stopped, `.ts.net` queries will fail; public DNS continues to work normally.

---

## Step 5 — Prevent background kills

Both apps must stay alive for the connection to hold.

1. **Settings → Apps → InviZible Pro → Battery** → set to **Unrestricted**.
2. **Settings → Apps → TailSocks → Battery** → set to **Unrestricted**.
3. On Pixel with Android 12+, also check **Settings → Battery → Adaptive preferences** and
   confirm neither app is listed as "Restricted."

---

## Step 6 — Verify

1. Start DNSCrypt in InviZible (add Tor if you want it). Then start TailSocks.
2. Open the app you assigned to the proxy in Step 2.
3. Test with a bare Tailscale IP first (`100.x.y.z`) — this bypasses DNS entirely and confirms
   the SOCKS5 tunnel is working.
4. Then test a MagicDNS name (e.g. `ssh user@my-laptop.ts.net`) to confirm forwarding rules work.
5. From any other Tailnet node, run `tailscale status` — your phone should appear as connected.

---

## FAQ

### Can I route all internet traffic through a Tailscale exit node?

Yes. In InviZible's proxy app list (Step 2, step 5), add all the apps you want routed to the exit
node. In TailSocks, select an **Exit Node** under **Peers**. Traffic from those apps will leave
your phone encrypted to the exit node; DNS for public names is still protected by DNSCrypt.

### What about Tor — do they conflict?

No. InviZible routes apps to Tor or to the proxy based on the per-app settings you configure.
An app assigned to the SOCKS5 proxy goes to TailSocks. An app assigned to Tor goes through Tor.
An app with neither assignment gets DNSCrypt-protected clearnet. The three paths are independent.

### Does MagicDNS work when Tor is active?

Yes, as long as you added the forwarding rule in Step 4. DNSCrypt-proxy resolves `.ts.net` by
forwarding the query to `127.0.0.1:1053` — a local loopback hop that never touches Tor.

### TailSocks' DNS proxy is not reachable from InviZible

This happens when TailSocks is started *after* DNSCrypt is already running. Restart DNSCrypt
inside InviZible (Stop → Start) after TailSocks shows **Connected** to pick up the local listener.
