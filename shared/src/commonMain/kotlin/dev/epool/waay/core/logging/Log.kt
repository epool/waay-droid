package dev.epool.waay.core.logging

import co.touchlab.kermit.Logger

/** App-wide Kermit logger. Expected failures (e.g. speech) are logged, never thrown. */
internal val log: Logger = Logger.withTag("Waay")
