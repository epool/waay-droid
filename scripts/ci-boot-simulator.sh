#!/usr/bin/env bash
# Boots the named simulator on the newest installed iOS runtime and prints its UDID. The boot carries
# on in the background, so CI can build the app while the simulator starts (ADR-011).
# Usage: scripts/ci-boot-simulator.sh "iPhone 17"
set -euo pipefail
name="$1"
udid=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
name = sys.argv[1]
devices = json.load(sys.stdin)["devices"]

def version(runtime):  # com.apple.CoreSimulator.SimRuntime.iOS-26-4 -> (26, 4)
    return tuple(int(part) for part in runtime.rsplit(".", 1)[1].split("-")[1:])

candidates = [
    (version(runtime), device["udid"])
    for runtime, runtime_devices in devices.items()
    if ".iOS-" in runtime
    for device in runtime_devices
    if device["name"] == name
]
if not candidates:
    sys.exit(f"No available {name} simulator")
print(max(candidates)[1])
' "$name")
xcrun simctl boot "$udid" 2>/dev/null || true  # already booted is fine
echo "$udid"
