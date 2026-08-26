#!/bin/bash
set -e
cd "$(dirname "$0")"
npm install
npx shadow-cljs release app
echo "Serving on http://localhost:7294"
open http://localhost:7294 2>/dev/null || true
python3 -m http.server 7294 --directory public
