package dev.epool.waay.fakes

import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit

/**
 * A time source where every reading is one second after the previous one, so consecutive player
 * actions in a test are never closer than the answer cooldown. Use `TestTimeSource` to freeze time.
 */
internal class AdvancingTimeSource : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
    private var now = 0L

    override fun read(): Long {
        now += 1_000
        return now
    }
}
