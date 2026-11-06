#!/bin/sh
set -eu
# Build against the exact TeslaMate API revision used by the existing service.
# The extension adds GET routes and does not alter upstream endpoints or database schema.
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR=${MERIDIAN_BUILD_DIR:-/tmp/meridian-teslamateapi-build}
mkdir -p "$BUILD_DIR"
if [ ! -f "$BUILD_DIR/go.mod" ]; then
 curl -fsSL https://github.com/tobiasehlert/teslamateapi/archive/7c3d3a628f50009174b0f2913ccf34c58c4edcd6.tar.gz | tar xz --strip-components=1 -C "$BUILD_DIR"
fi
cp "$ROOT/meridian.go" "$BUILD_DIR/src/meridian.go"
cp "$ROOT/meridian_test.go" "$BUILD_DIR/src/meridian_test.go"
python3 - "$BUILD_DIR/src/webserver.go" <<'PY'
import pathlib,sys
p=pathlib.Path(sys.argv[1]);s=p.read_text()
if 'registerMeridian(v1)' not in s:
 anchor='v1 := api.Group("/v1")'
 assert s.count(anchor)==1
 s=s.replace(anchor,anchor+'\n\t\tregisterMeridian(v1)')
 p.write_text(s)
PY
cd "$BUILD_DIR"
GOTOOLCHAIN=go1.26.4 CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags="-w -s -X main.apiVersion=1.25.0-meridian.1" -o "$BUILD_DIR/teslamateapi-meridian" ./src
