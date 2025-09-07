
package schwalbe.penilee.engine.input

import org.joml.*

class VrController {

    companion object {
        val LEFT = VrController()
        val RIGHT = VrController()

        internal val PRESSED = mutableSetOf<VrController.Button>()
        internal var WAS_PRESSED = mutableSetOf<VrController.Button>()

        fun startFrame() {
            VrController.WAS_PRESSED = VrController.PRESSED.toMutableSet()
        }
    }


    enum class Button {
        A, B, X, Y, STICK_L, STICK_R, MENU;

        fun press() { VrController.PRESSED.add(this) }
        fun release() { VrController.PRESSED.remove(this) }
    
        val isPressed: Boolean
            get() = VrController.PRESSED.contains(this)

        val wasReleased: Boolean
            get() = VrController.WAS_PRESSED.contains(this) && !this.isPressed
    }


    var trigger: Float = 0f
    var squeeze: Float = 0f
    var stick: Vector2fc = Vector2f()
    
    var gripPos: Vector3fc = Vector3f()
    var gripDir: Vector3fc = Vector3f()
    var gripUp: Vector3fc = Vector3f()
    var aimPos: Vector3fc = Vector3f()
    var aimDir: Vector3fc = Vector3f()
    var aimUp: Vector3fc = Vector3f()

}