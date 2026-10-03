#!/usr/bin/env bash
# Lints the iOS sources with Apple's swift-format (bundled with Xcode), using the repo's
# .swift-format. Gradle-independent so the CI iOS job and contributors can run it directly.
# Fix findings with: xcrun swift-format format --in-place --recursive iosApp/iosApp iosApp/WaayTests iosApp/WaayUITests
set -euo pipefail
cd "$(dirname "$0")/.."
xcrun swift-format lint --strict --recursive iosApp/iosApp iosApp/WaayTests iosApp/WaayUITests
