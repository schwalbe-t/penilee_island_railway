
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.input.Key
import schwalbe.penilee.level.loadLevelConfigs
import schwalbe.penilee.level.Level
import schwalbe.penilee.resources.loadAllResources

val DEFAULT_FOV: Float = 90.degrees
var CURRENT_LEVEL: Level? = null

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    loadAllResources()
    val levels: List<Level.Serialized> = loadLevelConfigs()
    val renderer = Renderer()
    CURRENT_LEVEL = levels[0].toLevel(renderer)
    window.show()
    window.makeFullscreen(false)
    val update: (Boolean) -> (Float, Camera) -> Unit = { inVr -> { dt, winCam ->
        if(Key.F11.wasReleased) {
            window.makeFullscreen(!window.isFullscreen) 
        }
        CURRENT_LEVEL?.update(dt, inVr, winCam)
        renderer.clearShadows()
        CURRENT_LEVEL?.renderShadows(renderer, dt)
    } }
    val render: (Camera, Framebuffer, Float) -> Unit = { cam, dest, dt ->
        renderer.beginRender(dest)
        CURRENT_LEVEL?.render(renderer, cam, dt)
        renderer.displayRender(dest)
    }
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(window, update(true), render)
    }
    if(!vrContext) {
        window.enableVSync()
        window.runLoop(DEFAULT_FOV, update(false), render)
    }
    renderer.destroy()
}