
package schwalbe.penilee.engine.input

import org.lwjgl.glfw.GLFW.*
import org.joml.*

val GLFW_MOUSE_BUTTON_MAP: Map<Int, Mouse> = mapOf(
    GLFW_MOUSE_BUTTON_LEFT to Mouse.LEFT,
    GLFW_MOUSE_BUTTON_MIDDLE to Mouse.MIDDLE,
    GLFW_MOUSE_BUTTON_RIGHT to Mouse.RIGHT
)

enum class Mouse {
    LEFT,
    MIDDLE,
    RIGHT;

    companion object {
        internal val PRESSED: MutableSet<Mouse> = mutableSetOf()
        internal var WAS_PRESSED: MutableSet<Mouse> = mutableSetOf()
        
        var pos: Vector2fc = Vector2f()
            private set
        var dpos: Vector2fc = Vector2f()
            private set

        var cursorHidden: Boolean = false

        fun startFrame() {
            Mouse.WAS_PRESSED = Mouse.PRESSED.toMutableSet()
            Mouse.dpos = Vector2f()
        }

        fun moveTo(p: Vector2fc) {
            Mouse.dpos = Vector2f(Mouse.dpos).add(p).sub(Mouse.pos)
            Mouse.pos = Vector2f(p)
        }

    }

    fun press() { Mouse.PRESSED.add(this) }
    fun release() { Mouse.PRESSED.remove(this) }

    val isPressed: Boolean
        get() = Mouse.PRESSED.contains(this)

    val wasReleased: Boolean
        get() = Mouse.WAS_PRESSED.contains(this) && !this.isPressed
}