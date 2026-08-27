#!/bin/bash
# All-ABI Windows variant of build.sh (windows-x86_64 NDK prebuilts).
# Reuses the already-downloaded+patched tailscale_src from build_x86_64_win.sh.
set -e
set -o pipefail

export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-C:/Android/Sdk/ndk/23.1.7779620}"
export PATH="$USERPROFILE/go/bin:$PATH"

TS_VERSION="v1.98.3"
NDK_BIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/windows-x86_64/bin"

echo "[1/4] Preparing and Patching Tailscale sources..."
if [ ! -d "tailscale_src" ]; then
    echo "-> Downloading sources..."
    curl -sL "https://github.com/tailscale/tailscale/archive/refs/tags/${TS_VERSION}.tar.gz" | tar -xz
    mv tailscale-${TS_VERSION#v} tailscale_src
    echo "-> Applying atomic patches..."
    for p in patches/*.patch; do
        if [ -f "$p" ]; then
            echo "Applying patch: $(basename "$p")"
            patch -p1 -d tailscale_src < "$p"
        fi
    done
    echo "Sources patched successfully."
else
    echo "-> Sources already exist and patched. Skipping download."
fi

TAGS="ts_omit_systray,ts_omit_kube,ts_omit_aws,ts_omit_bird,ts_omit_qrcodes,ts_omit_desktop_sessions,ts_omit_dbus,ts_omit_networkmanager,ts_omit_resolved,ts_omit_sdnotify,ts_omit_tpm,ts_omit_logtail,ts_omit_synology,ts_omit_syspolicy,ts_omit_ssh,ts_omit_iptables,ts_omit_tap,ts_omit_linuxdnsfight,ts_omit_captiveportal,ts_omit_appconnectors,ts_omit_completion,ts_omit_completion_scripts,ts_omit_oauthkey"

echo "[2/4] Compiling binaries in PIE mode (all ABIs)..."
cd tailscale_src
mkdir -p tmp
go mod tidy

export CGO_ENABLED=1

echo "-> ARM64 daemon+cli..."
export CC="${NDK_BIN}/aarch64-linux-android21-clang.cmd"
GOOS=android GOARCH=arm64 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_arm64.so ./cmd/tailscaled
GOOS=android GOARCH=arm64 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_cli_arm64.so ./cmd/tailscale

echo "-> ARM 32-bit daemon+cli..."
export CC="${NDK_BIN}/armv7a-linux-androideabi21-clang.cmd"
GOOS=android GOARCH=arm GOARM=7 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_arm.so ./cmd/tailscaled
GOOS=android GOARCH=arm GOARM=7 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_cli_arm.so ./cmd/tailscale

echo "-> x86 32-bit daemon+cli..."
export CC="${NDK_BIN}/i686-linux-android21-clang.cmd"
GOOS=android GOARCH=386 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_x86.so ./cmd/tailscaled
GOOS=android GOARCH=386 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_cli_x86.so ./cmd/tailscale

echo "-> x86_64 daemon+cli..."
export CC="${NDK_BIN}/x86_64-linux-android21-clang.cmd"
GOOS=android GOARCH=amd64 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_x86_64.so ./cmd/tailscaled
GOOS=android GOARCH=amd64 go build -buildmode=pie -tags "$TAGS" -ldflags="-s -w -checklinkname=0" -o tmp/libtailscale_cli_x86_64.so ./cmd/tailscale

cd ..

echo "[3/4] Building appctr.aar (all 4 targets)..."
go mod tidy
mkdir -p tmp
unset CC
gomobile bind -ldflags="-s -w -buildid= -checklinkname=0 -X appctr.coreVersion=$TS_VERSION" -trimpath -target="android/arm,android/arm64,android/386,android/amd64" -androidapi 21 -tags "$TAGS" -o tmp/appctr.aar -v .

echo "[4/4] Copying binaries to jniLibs..."
for pair in "arm64-v8a:arm64" "armeabi-v7a:arm" "x86:x86" "x86_64:x86_64"; do
    abi="${pair%%:*}"; suf="${pair##*:}"
    mkdir -p ../app/src/main/jniLibs/$abi
    cp tailscale_src/tmp/libtailscale_${suf}.so ../app/src/main/jniLibs/$abi/libtailscale.so
    cp tailscale_src/tmp/libtailscale_cli_${suf}.so ../app/src/main/jniLibs/$abi/libtailscale_cli.so
done

echo "BUILD-ALL-COMPLETE"
