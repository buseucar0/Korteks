#!/usr/bin/env bash
# Ekranları emülatörsüz PNG olarak çizer (Paparazzi).
# Kullanım: ./onizle.sh            → hepsi
#           ./onizle.sh stroop     → adında "stroop" geçen önizlemeler
set -e
FILTER="${1:-}"
if [ -n "$FILTER" ]; then
    ./gradlew -q recordPaparazziDebug --tests "*ScreenPreviewTest.*${FILTER}*"
else
    ./gradlew -q recordPaparazziDebug
fi
echo ">> Önizlemeler hazır:"
ls -1 app/src/test/snapshots/images/ | sed 's/^com.buse.korteks.ui_ScreenPreviewTest_/   /'
echo ">> Klasör: app/src/test/snapshots/images/"
