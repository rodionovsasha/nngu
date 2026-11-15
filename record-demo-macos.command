#!/bin/bash
# Воспроизводимая запись двух реальных UI, без захвата экрана и без звука.
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"
if [[ "$(uname -s)" != Darwin ]]; then
  echo 'Этот скрипт использует AVFoundation и требует macOS.' >&2
  exit 1
fi
command -v xcrun >/dev/null || { echo 'Нужны Xcode Command Line Tools (xcode-select --install).' >&2; exit 1; }
mkdir -p build/demo-video docs/videos docs/images
FRAMES_DIR="$(mktemp -d "$PROJECT_DIR/build/demo-video/frames.XXXXXX")"
./gradlew :javafx:run --args="--demo-frames \"$FRAMES_DIR\"" \
  :compose:run --args="--demo-frames \"$FRAMES_DIR\"" --console=plain
xcrun swiftc -parse-as-library -module-cache-path build/demo-video/swift-cache \
  tools/encode-demo.swift -o build/demo-video/encode-demo
build/demo-video/encode-demo "$FRAMES_DIR" "$FRAMES_DIR/demo.mp4" "$FRAMES_DIR/poster.png"
mv -f "$FRAMES_DIR/demo.mp4" docs/videos/quantum-well-demo.mp4
mv -f "$FRAMES_DIR/poster.png" docs/images/demo-video-poster.png
rm -rf -- "$FRAMES_DIR"
echo 'Готово: docs/videos/quantum-well-demo.mp4 — 30 секунд, без звука.'
