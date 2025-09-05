
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.resources.loadAllResources

val DEFAULT_FOV: Float = 90.degrees
var CURRENT_LEVEL: Level = Level()

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    loadAllResources()
    window.show()
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(
            window,
            { dt -> CURRENT_LEVEL.update(dt) },
            { cam, dest, dt -> CURRENT_LEVEL.render(cam, dest, dt) }
        )
    }
    if(!vrContext) {
        window.runLoop(
            DEFAULT_FOV.toFloat(),
            { dt -> CURRENT_LEVEL.update(dt) },
            { cam, dest, dt -> CURRENT_LEVEL.render(cam, dest, dt) }
        )
    }
}