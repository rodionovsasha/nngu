#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
exec python3 python/potential_well_visualizer.py
