#!/bin/bash
set -e
cd "$(dirname "$0")"
npm install
npx shadow-cljs release app
echo "Serving on http://localhost:7293"
open http://localhost:7293 2>/dev/null || true
python3 -m http.server 7293 --directory public
