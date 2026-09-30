#!/usr/bin/env bash
# Kodu derle → emülatöre/telefona yükle → uygulamayı başlat.
# Gömülüdeki "build + flash + reset" döngüsünün karşılığı.
set -e
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

# Bağlı cihaz yoksa emülatörü aç
if [ "$("$ADB" get-state 2>/dev/null)" != "device" ]; then
    echo ">> Emülatör başlatılıyor..."
    nohup "$ANDROID_HOME/emulator/emulator" -avd oyun_pixel -no-snapshot-save -no-boot-anim >/dev/null 2>&1 &
    timeout 120 "$ADB" wait-for-device || { echo "!! Emülatör 2 dakikada bağlanmadı"; exit 1; }
    for _ in $(seq 1 60); do
        # timeout 5: donmuş emülatörde adb sonsuza kadar beklemesin
        [ "$(timeout 5 "$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
        sleep 3
    done
fi
# Emülatör açık ama donmuşsa burada anlaşılır
timeout 10 "$ADB" shell true || { echo "!! Cihaz yanıt vermiyor. Emülatörü kapatıp tekrar dene: adb emu kill"; exit 1; }

echo ">> Derleniyor..."
./gradlew -q assembleDebug
echo ">> Yükleniyor..."
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
"$ADB" shell am force-stop com.buse.korteks
"$ADB" shell am start -n com.buse.korteks/.MainActivity >/dev/null
echo ">> Hazır! Emülatör penceresine bak."
