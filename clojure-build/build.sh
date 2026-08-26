#!/bin/bash
set -e
cd "$(dirname "$0")"
npm install
mkdir -p out
npm run bundle
CP=$(clojure -A:build -Spath 2>/dev/null)
mkdir -p classes
javac -cp "$CP" -d classes \
  src/com/google/javascript/jscomp/ShadowAccess.java \
  src/shadow/build/closure/NodeEnvInlinePass.java \
  src/shadow/build/closure/ReplaceCLJSConstants.java
clojure -M:build
echo "Serving on http://localhost:7292"
open http://localhost:7292 2>/dev/null || true
python3 -m http.server 7292
