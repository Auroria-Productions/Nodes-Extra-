#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
DEPS="${DEPS_DIR:-$ROOT/lib}"
CORE="${NODES_JAR:-$ROOT/lib/milzzynodes.jar}"
if [[ ! -f "$DEPS/paper-api-1.21.jar" || ! -f "$CORE" ]]; then
  echo 'Paper API or milzzynodes JAR not available. Set DEPS_DIR and NODES_JAR.' >&2; exit 1
fi
rm -rf "$ROOT/build" && mkdir -p "$ROOT/build/classes" "$ROOT/build/tests" "$ROOT/build/stubs" "$ROOT/dist"
javac --release 21 -d "$ROOT/build/stubs" "$ROOT/src/main/stubs/net/md_5/bungee/api/chat/BaseComponent.java"
javac --release 21 -Xlint:unchecked -cp "$CORE:$DEPS/*:$ROOT/build/stubs" -d "$ROOT/build/classes" $(find "$ROOT/src/main/java" -name '*.java' | sort)
cp "$ROOT/src/main/resources/"* "$ROOT/build/classes/"
jar cf "$ROOT/dist/Nodes-Extra.jar" -C "$ROOT/build/classes" .
javac --release 21 -cp "$ROOT/build/classes:$CORE:$DEPS/*:$ROOT/build/stubs" -d "$ROOT/build/tests" $(find "$ROOT/src/test/java" -name '*.java' | sort)
for c in SupplyGraphTest ElectionTest SerializationTest ApiCompatibilityTest NodesCommandBridgeRoutingTest; do
  java -cp "$ROOT/build/tests:$ROOT/build/classes:$CORE:$DEPS/*:$ROOT/build/stubs" "com.auroria.nodesextra.$c"
done
unzip -t "$ROOT/dist/Nodes-Extra.jar" | tail -1
unzip -p "$ROOT/dist/Nodes-Extra.jar" plugin.yml | head -6
echo "OK: $ROOT/dist/Nodes-Extra.jar"
