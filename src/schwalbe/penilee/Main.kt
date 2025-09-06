
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.loadAllResources

val DEFAULT_FOV: Float = 90.degrees
var CURRENT_LEVEL: Level? = null

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    loadAllResources()
    val renderer = Renderer()
    CURRENT_LEVEL = createTestLevel(renderer)
    window.show()
    val update: (Float) -> Unit = { dt ->
        CURRENT_LEVEL?.update(dt)
        renderer.clearShadows()
        CURRENT_LEVEL?.renderShadows(renderer, dt)
    }
    val render: (Camera, Framebuffer, Float) -> Unit = { cam, dest, dt ->
        renderer.beginRender(dest)
        CURRENT_LEVEL?.render(renderer, cam, dt)
        renderer.displayRender(dest)
    }
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(window, update, render)
    }
    if(!vrContext) {
        window.runLoop(DEFAULT_FOV, update, render)
    }
    renderer.destroy()
}