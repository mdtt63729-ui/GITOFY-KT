#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Cross-compile the real git (C) for Android and drop it into the app as
# jniLibs/<abi>/libgit.so  —  the Termux approach, so the app gets native-git
# pack/delta speed instead of JGit's.
#
# Naming it libgit.so (not "git") is deliberate: Android extracts any lib*.so
# from jniLibs into applicationInfo.nativeLibraryDir, which is one of the few
# places left where the OS permits executing a file on Android 10+.
#
# Requires: NDK r26+, curl, tar, make, a POSIX shell. Run from the repo root:
#     tools/build-git-android.sh arm64-v8a
# ---------------------------------------------------------------------------
set -euo pipefail

ABI="${1:-arm64-v8a}"
API="${API:-26}"
NDK="${ANDROID_NDK_HOME:-${NDK_HOME:-}}"

GIT_VERSION="${GIT_VERSION:-2.56.0}"   # latest feature release (2026-09-28)
CURL_VERSION="${CURL_VERSION:-8.22.0}"
OPENSSL_VERSION="${OPENSSL_VERSION:-3.6.5}"
ZLIB_VERSION="${ZLIB_VERSION:-1.3.2}"
PCRE2_VERSION="${PCRE2_VERSION:-10.49}"
EXPAT_VERSION="${EXPAT_VERSION:-2.6.2}"

if [[ -z "$NDK" || ! -d "$NDK" ]]; then
  echo "ERROR: set ANDROID_NDK_HOME to your NDK (r26+)." >&2
  exit 1
fi

# OpenSSL's android config reads **ANDROID_NDK_ROOT** (not _HOME) and requires the
# compiler it probes to live *inside* that NDK (^$ndk/.*/prebuilt/<host>/). A CI image
# can have a different NDK already exported, so pin ANDROID_NDK_ROOT to the one we use
# — otherwise OpenSSL falls through to its gcc branch and dies with
# "no NDK aarch64-linux-android-gcc on $PATH".
export ANDROID_NDK_ROOT="$NDK"
export ANDROID_NDK_HOME="$NDK"
export ANDROID_NDK="$NDK"

case "$ABI" in
  arm64-v8a)   TARGET=aarch64-linux-android ;;
  armeabi-v7a) TARGET=armv7a-linux-androideabi ;;
  *) echo "ERROR: unsupported ABI '$ABI' (use arm64-v8a or armeabi-v7a)" >&2; exit 1 ;;
esac

HOST_TAG=linux-x86_64
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
SYSROOT="$TOOLCHAIN/sysroot"
export PATH="$TOOLCHAIN/bin:$PATH"

CC="${TARGET}${API}-clang"
CXX="${TARGET}${API}-clang++"
AR="llvm-ar"
RANLIB="llvm-ranlib"
STRIP="llvm-strip"

# OpenSSL's android-* config hardcodes a "<triple>-gcc" compiler (and -ar/-ranlib),
# but the modern NDK ships clang only. Drop tiny shims on PATH that forward the
# expected names to the real clang/llvm tools, so OpenSSL's configure finds them.
# Put the shims *inside the NDK*: OpenSSL only accepts a probed compiler whose path
# matches ^$ndk/.*/prebuilt/<host>/. Fall back to a local dir if the NDK isn't writable.
SHIM="$TOOLCHAIN/bin"
if ! ( : > "$SHIM/.gitofy_probe" ) 2>/dev/null; then SHIM="$(pwd)/shim-bin"; fi
mkdir -p "$SHIM"
rm -f "$TOOLCHAIN/bin/.gitofy_probe" 2>/dev/null || true
shim() { # <expected-name> <real-cmd>
  local real; real="$(command -v "$2" 2>/dev/null)" || return 0
  printf '#!/bin/sh\nexec "%s" "$@"\n' "$real" > "$SHIM/$1" 2>/dev/null || return 0
  chmod +x "$SHIM/$1" 2>/dev/null || true
}
shim "${TARGET}-gcc"    "$CC"
shim "${TARGET}-cc"     "$CC"
shim "${TARGET}-g++"    "$CXX"
shim "${TARGET}-c++"    "$CXX"
shim "${TARGET}-ar"     "$AR"
shim "${TARGET}-ranlib" "$RANLIB"
shim "${TARGET}-strip"  "$STRIP"
shim "${TARGET}-nm"     "llvm-nm"
shim "${TARGET}-objcopy" "llvm-objcopy"
# Some OpenSSL/curl targets spell the triple differently — cover the arm variant too.
if [[ "$ABI" == armeabi-v7a ]]; then
  shim "arm-linux-androideabi-gcc" "$CC"
  shim "arm-linux-androideabi-ar"  "$AR"
fi
export PATH="$SHIM:$PATH"

WORK="$(pwd)/.git-build/$ABI"
PREFIX="$WORK/prefix"
mkdir -p "$WORK" "$PREFIX"
cd "$WORK"

fetch() { # url  tarball
  [[ -f "$2" ]] || curl -fL "$1" -o "$2"
}

echo "== 1/5 zlib =="
fetch "https://zlib.net/zlib-${ZLIB_VERSION}.tar.gz" "zlib.tar.gz"
rm -rf zlib-*; tar xf zlib.tar.gz
( cd zlib-${ZLIB_VERSION}
  CHOST=$TARGET ./configure --prefix="$PREFIX" --static
  make -j"$(nproc)" && make install )

echo "== 2/5 openssl (static) =="
fetch "https://www.openssl.org/source/openssl-${OPENSSL_VERSION}.tar.gz" "openssl.tar.gz"
rm -rf openssl-*; tar xf openssl.tar.gz
( cd openssl-${OPENSSL_VERSION}
  ./Configure "android-$( [[ $ABI == arm64-v8a ]] && echo arm64 || echo arm )" \
      -D__ANDROID_API__="$API" no-shared no-tests \
      --prefix="$PREFIX" --openssldir="$PREFIX/ssl"
  make -j"$(nproc)" && make install_sw )

echo "== 3/5 pcre2 =="
fetch "https://github.com/PCRE2Project/pcre2/releases/download/pcre2-${PCRE2_VERSION}/pcre2-${PCRE2_VERSION}.tar.gz" "pcre2.tar.gz"
rm -rf pcre2-*; tar xf pcre2.tar.gz
( cd pcre2-${PCRE2_VERSION}
  ./configure --host="$TARGET" CC="$CC" CXX="$CXX" AR="$AR" RANLIB="$RANLIB" \
      --prefix="$PREFIX" --disable-shared --enable-static
  make -j"$(nproc)" && make install )

echo "== 4/5 curl (static, openssl + zlib) =="
fetch "https://curl.se/download/curl-${CURL_VERSION}.tar.gz" "curl.tar.gz"
rm -rf curl-*; tar xf curl.tar.gz
( cd curl-${CURL_VERSION}
  ./configure --host="$TARGET" CC="$CC" AR="$AR" RANLIB="$RANLIB" \
      --prefix="$PREFIX" --with-openssl="$PREFIX" --with-zlib="$PREFIX" \
      --disable-shared --enable-static --disable-ldap --disable-rtsp \
      --disable-dict --disable-telnet --disable-tftp --disable-pop3 \
      --disable-imap --disable-smtp --without-libidn2 --without-librtmp \
      --without-brotli --without-zstd --without-libpsl
  make -j"$(nproc)" && make install )

echo "== 5/5 git =="
fetch "https://mirrors.edge.kernel.org/pub/software/scm/git/git-${GIT_VERSION}.tar.gz" "git.tar.gz"
rm -rf git-${GIT_VERSION}; tar xf git.tar.gz
( cd git-${GIT_VERSION}
  make -j"$(nproc)" \
    CC="$CC" AR="$AR" RANLIB="$RANLIB" STRIP="$STRIP" \
    CFLAGS="-O2 -fPIE" LDFLAGS="-pie -static-libgcc" \
    NO_GETTEXT=1 NO_TCLTK=1 NO_PERL=1 NO_PYTHON=1 NO_INSTALL_HARDLINKS=1 \
    NO_PTHREADS=1 NO_EXPAT=1 NO_ICONV=1 CSPRNG_METHOD=openssl \
    CURL_LDFLAGS="-lcurl -lssl -lcrypto -lz" \
    CURL_CFLAGS="-I$PREFIX/include" \
    ZLIB_PATH="$PREFIX" PCRE2_PATH="$PREFIX" \
    OPENSSL_PATH="$PREFIX" CURL_PATH="$PREFIX" \
    EXPAT_PATH="$PREFIX" \
    prefix="$PREFIX" \
    all
  make prefix="$PREFIX" NO_PERL=1 NO_TCLTK=1 NO_PTHREADS=1 CSPRNG_METHOD=openssl install )

# The single static git binary is what we ship.
DEST="$(pwd)/../../app/src/main/jniLibs/$ABI"
mkdir -p "$DEST"
cp "$PREFIX/bin/git" "$DEST/libgit.so"
"$STRIP" --strip-unneeded "$DEST/libgit.so" || true
chmod 755 "$DEST/libgit.so"

echo
echo "DONE: $DEST/libgit.so"
ls -lh "$DEST/libgit.so"
echo "Verify: file $DEST/libgit.so   (expect: ELF ... AArch64 ... statically linked)"
