#!/usr/bin/env bash
# Ekranları emülatörsüz PNG olarak çizer (Paparazzi).
# Kullanım: ./onizle.sh            → hepsi
#           ./onizle.sh stroop     → adında "stroop" geçen önizlemeler
#           ./onizle.sh tablet     → yatay tablet önizlemeleri
set -e
FILTER="${1:-}"
if [ -n "$FILTER" ]; then
    ./gradlew -q recordPaparazziDebug --tests "*PreviewTest.*${FILTER}*"
else
    ./gradlew -q recordPaparazziDebug
fi
echo ">> Önizlemeler hazır:"
ls -1 app/src/test/snapshots/images/ | sed -E 's/^com.buse.korteks.ui_[A-Za-z]+PreviewTest_/   /'
echo ">> Klasör: app/src/test/snapshots/images/"
