#!/usr/bin/env bash
# Turns the video recorded by `npm run screenshots` into docs/images/hero-demo.{mp4,gif}.
# Needs ffmpeg. Run from the e2e directory: npm run screenshots && ./screenshots/render-demo.sh
set -euo pipefail

cd "$(dirname "$0")/.."
video=$(find test-results/screenshots -name '*.webm' | head -n 1)
[ -n "$video" ] || { echo "No recording found — run 'npm run screenshots' first." >&2; exit 1; }
out=../docs/images

ffmpeg -y -loglevel error -i "$video" -an -vf "scale=1280:-2" -c:v libx264 -pix_fmt yuv420p -movflags +faststart "$out/hero-demo.mp4"
ffmpeg -y -loglevel error -i "$video" -vf "fps=8,scale=760:-1:flags=lanczos,palettegen=max_colors=128" /tmp/newscore-palette.png
ffmpeg -y -loglevel error -i "$video" -i /tmp/newscore-palette.png -lavfi "fps=8,scale=760:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=4" "$out/hero-demo.gif"
rm -f /tmp/newscore-palette.png
ls -la "$out"/hero-demo.*
