#!/usr/bin/env bash
# WorkPlanner derleme yardımcısı.
#
#   tools/build.sh                    # debug APK
#   tools/build.sh :app:assembleDebug # aynı şey
#   tools/build.sh :app:assembleRelease
#
# Proje, macOS'un AppleDouble "._*" dosyaları ürettiği bir exFAT USB diskte
# duruyorsa AGP'nin resource/dex birleştirme adımları bozulur. Bu betik:
#   1. derlemeden önce mevcut "._*" dosyalarını siler,
#   2. disk yerel değilse derleme çıktısını APFS üzerindeki geçici bir dizine
#      yönlendirir (WORKPLANNER_BUILD_DIR),
#   3. üretilen APK'yı tekrar app/build/outputs/apk/debug/ altına kopyalar.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# --- Java ---------------------------------------------------------------
# Gradle 8.6 + AGP 8.2 JDK 17-21 ister; bu aralıktaki ilk kurulumu seç.
jdk_major() {
  local version
  version="$(sed -n 's/^JAVA_VERSION="\([0-9][0-9._]*\)".*/\1/p' "$1/release" 2>/dev/null)"
  [[ -n "$version" ]] || return 1
  echo "${version%%.*}"
}

java_supported() {
  local home="$1" major
  [[ -x "$home/bin/java" ]] || return 1
  major="$(jdk_major "$home" 2>/dev/null)" || return 1
  [[ "$major" =~ ^[0-9]+$ ]] || return 1
  (( major >= 17 && major <= 21 ))
}

if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "$HOME/.gradle/jdks"/*/*/Contents/Home \
    "$HOME/Library/Java/JavaVirtualMachines"/*/Contents/Home; do
    if java_supported "$candidate"; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "HATA: JDK 17-21 bulunamadı. JAVA_HOME değişkenini ayarlayın." >&2
  exit 1
fi

# --- AppleDouble dosyalarını temizle -------------------------------------
find "$PROJECT_DIR" -name '._*' -prune -exec rm -f {} + 2>/dev/null || true

# --- Yerel olmayan diskte çıktıyı taşı -----------------------------------
GRADLE_ARGS=()
BUILD_ROOT=""
FS_TYPE="$(stat -f '%T' . 2>/dev/null || echo unknown)"
if [[ "$FS_TYPE" != "apfs" && "$FS_TYPE" != "hfs" ]]; then
  BUILD_ROOT="${WORKPLANNER_BUILD_DIR:-${TMPDIR:-/tmp/}workplanner-build}"
  export WORKPLANNER_BUILD_DIR="$BUILD_ROOT"
  GRADLE_ARGS+=(--project-cache-dir "${TMPDIR:-/tmp/}workplanner-gradle-cache")
fi

TASKS=("$@")
if [[ ${#TASKS[@]} -eq 0 ]]; then
  TASKS=(:app:assembleDebug)
fi

./gradlew ${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"} "${TASKS[@]}"

# --- APK'yı proje içine geri kopyala ------------------------------------
if [[ -n "$BUILD_ROOT" && -f "$BUILD_ROOT/app/outputs/apk/debug/app-debug.apk" ]]; then
  mkdir -p app/build/outputs/apk/debug
  cat "$BUILD_ROOT/app/outputs/apk/debug/app-debug.apk" > app/build/outputs/apk/debug/app-debug.apk
  echo "APK: $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
fi

find "$PROJECT_DIR" -name '._*' -prune -exec rm -f {} + 2>/dev/null || true
