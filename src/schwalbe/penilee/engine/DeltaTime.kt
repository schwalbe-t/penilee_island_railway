
package schwalbe.penilee.engine

import kotlin.time.*

internal class DeltaTimeState {

    private val clock: Clock = Clock.System
    private var lastFrame: Instant = this.clock.now()

    fun computeDeltaTime(): Float {
        val frame: Instant = this.clock.now()
        val delta: Duration = frame - this.lastFrame
        val deltaSecs: Float = delta.toDouble(DurationUnit.SECONDS).toFloat()
        this.lastFrame = frame
        return deltaSecs
    }

}