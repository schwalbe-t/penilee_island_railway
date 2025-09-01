
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import org.joml.*
import kotlin.math.*

val DEFAULT_FOV: Double = (PI * 2.0 / 360.0) * 60.0

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(::update, ::render)
    }
    if(!vrContext) {
        window.show()
        window.runLoop(DEFAULT_FOV.toFloat(), ::update, ::render)
    }
}

fun update() {

}

fun render(screen: Camera, dest: Framebuffer) {
    dest.clear(Vector4f(abs(screen.dir.x()), abs(screen.dir.y()), abs(screen.dir.z()), 1f))
}