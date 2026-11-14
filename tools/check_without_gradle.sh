#!/usr/bin/env bash
# Compiles the mod and its tests with plain javac against the jars Gradle already downloaded, then runs the JUnit tests.
# No Gradle, no game: light enough to run while Minecraft is open.   bash tools/check_without_gradle.sh [other source root]
# With a source root (e.g. ../fortressboxes/src/main/java) it only compiles that folder: a type check of another mod.
set -e
cd "$(dirname "$0")/.."
JAVA_HOME_DIR="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
G="$HOME/.gradle/caches"
OUT=out-check
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes"

{
  echo "$G/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.0_mapped_official_1.20.1/forge-1.20.1-47.3.0_mapped_official_1.20.1.jar"
  find "$G/forge_gradle/maven_downloader" "$G/modules-2/files-2.1" -name "*.jar" 2>/dev/null \
    | grep -viE "sources|javadoc|kotlin|androidx|com\.android|/forge/1\.|compose|jetbrains"
} > "$OUT/jars.txt"

CP=$(while read -r j; do cygpath -m "$j"; done < "$OUT/jars.txt" | paste -sd';' -)
printf -- '-cp\n"%s;%s"\n' "$(cygpath -m "$PWD/$OUT/classes")" "$CP" > "$OUT/cp.arg"

if [ -n "$1" ]; then
  find "$1" -name "*.java" | while read -r f; do cygpath -m "$f"; done > "$OUT/srcs.arg"
  "$JAVA_HOME_DIR/bin/javac" -J-Xmx1g -nowarn -proc:none -encoding UTF-8 -d "$OUT/classes" "@$OUT/cp.arg" "@$OUT/srcs.arg"
  echo "compiled $(wc -l < "$OUT/srcs.arg") files from $1 (javac only, not a Gradle build)"
  exit 0
fi

find src/main/java src/test/java tools -name "*.java" | while read -r f; do cygpath -m "$f"; done > "$OUT/srcs.arg"
"$JAVA_HOME_DIR/bin/javac" -J-Xmx1g -nowarn -proc:none -encoding UTF-8 -d "$OUT/classes" "@$OUT/cp.arg" "@$OUT/srcs.arg"
echo "compiled $(wc -l < "$OUT/srcs.arg") files (javac only, not a Gradle build)"
"$JAVA_HOME_DIR/bin/java" -Xmx512m "@$OUT/cp.arg" RunTests
