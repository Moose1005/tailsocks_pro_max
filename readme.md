<p align="center">
  <img src="docs/logo.svg" alt="TailSocks Icon" width="128" height="128" />
</p>

<h3 align="center">TailSocks </h3>
<h4 align="center">Pro Max </h4>

<p align="center">
  <strong>Advanced Tailscale Client for Android with Userspace Networking & Transparent TUN VPN</strong>
</p>

<p align="center">
  <strong>English</strong> | <a href="readme_ru.md">Русский</a>
</p>

<p align="center">
  <a href="https://github.com/bropines/tailsocks/releases/latest"><img src="https://img.shields.io/github/v/release/bropines/tailsocks?style=for-the-badge&logo=github&logoColor=white&label=Latest%20Release&color=2ea44f" alt="Latest Release" /></a>
  <a href="https://github.com/bropines/tailsocks/releases"><img src="https://img.shields.io/github/downloads/bropines/tailsocks/total?style=for-the-badge&logo=android&logoColor=white&label=Downloads&color=3ddc84" alt="Downloads" /></a>
  <a href="https://github.com/tailscale/tailscale/releases/tag/v1.98.3"><img src="https://img.shields.io/badge/Tailscale_Core-v1.98.3-blue?style=for-the-badge&logo=tailscale&logoColor=white" alt="Tailscale Core" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-BSD_3--Clause-orange?style=for-the-badge" alt="License" /></a>
</p>

<p align="center">
  <a href="https://github.com/bropines/tailsocks/releases/latest/download/app-release.apk">
    <img src="https://img.shields.io/badge/⬇_Download_APK-Release-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download Release APK" />
  </a>
  &nbsp;
  <a href="https://boosty.to/pinus">
    <img src="https://img.shields.io/badge/❤️_Donate-Boosty-f15f2c?style=for-the-badge" alt="Donate on Boosty" />
  </a>
  &nbsp;
  <a href="https://github.com/bropines/tailsocks/releases">
    <img src="https://img.shields.io/badge/⬇_All_Releases-GitHub-24292e?style=for-the-badge&logo=github&logoColor=white" alt="All Releases" />
  </a>
</p>

---

> ## This is a fork
>
> **[Moose1005/tailsocks_pro_max](https://github.com/Moose1005/tailsocks_pro_max)** - a fork of
> **[bropines/tailsocks](https://github.com/bropines/tailsocks)**, maintained as the TailSocks half
> of **[InviZible Pro Max](https://github.com/Moose1005/InviZible-Pro-Max)**.
>
> InviZible Pro Max adds Tailscale as a fourth module beside DNSCrypt, Tor and Purple I2P. Android
> allows only one `VpnService` at a time, so rather than running a second VPN it drives this build
> over a signature-protected IPC bridge and routes app traffic through the SOCKS5 proxy, while
> InviZible keeps the tunnel. No root required.
>
> ### What this fork adds on top of upstream
>
> | Change | Why |
> |---|---|
> | `InviZibleBridgeReceiver` | Status channel back to InviZible, resolved at runtime and addressed only to co-signed receivers |
> | `TailscaledService` exported behind signature-level `pan.alexander.tordnscrypt.BRIDGE_PERMISSION` | Lets InviZible start and stop it from its foreground context. Same-UID callers are exempt, so standalone use is unaffected |
> | `EXTRA_BRIDGE_SOCKS_ONLY` | Forces SOCKS-only operation so TUN never seizes the `VpnService` slot InviZible owns - honoured on a cold start *and* against an already-running instance |
> | `appctr/build_x86_64_win.sh`, `appctr/build_all_win.sh` | Windows build scripts for the Go core. Upstream's `build.sh` has Linux-only NDK paths |
> | [`docs/INVIZIBLE.md`](docs/INVIZIBLE.md) | Bridge setup and coexistence notes |
>
> Standalone TailSocks behaviour is deliberately untouched: every change above is inert unless
> InviZible drives the bridge.
>
> **TailSocks is bropines' work** - this fork exists
> only to integrate it. If you find it useful, support the original author on
> [Boosty](https://boosty.to/pinus) and star
> [the upstream repository](https://github.com/bropines/tailsocks).
contribution and is not affiliated with Tailscale Inc.*
