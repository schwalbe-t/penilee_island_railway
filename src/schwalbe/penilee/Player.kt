
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.Camera
import schwalbe.penilee.engine.input.*
import org.joml.*
import kotlin.math.*

val PLAYER_SPEED: Float = 3f // per second
val PLAYER_CAM_OFFSET: Vector3fc = Vector3f(0f, 1.5f, 0f)
val PLAYER_STICK_TURN_SPEED: Float = 180.degrees // per second

class Player(var position: Vector3f, val bounds: List<AABB>) {

    var angleX: Float = 0f
    var angleY: Float = 0f

    fun getGamepadDeltaPos(forward: Vector3fc, right: Vector3fc): Vector3f {
        return Vector3f()
            .add(Vector3f(right).mul(VrController.LEFT.stick.x()))
            .add(Vector3f(forward).mul(VrController.LEFT.stick.y()))
    }

    fun getKeyboardDeltaPos(forward: Vector3fc, right: Vector3fc): Vector3f {
        val dpos = Vector3f()
        if(Key.W.isPressed) { dpos.add(forward) }
        if(Key.A.isPressed) { dpos.sub(right) }
        if(Key.S.isPressed) { dpos.sub(forward) }
        if(Key.D.isPressed) { dpos.add(right) }
        return dpos
    }

    fun updatePosition(deltaTime: Float, windowCam: Camera) {
        val camView: Matrix4f = windowCam.computeView()
        val rightDir = Vector3f(
            camView.get(0, 0), camView.get(1, 0), camView.get(2, 0)
        )
        val forwardDir = Vector3f(
            camView.get(0, 2), camView.get(1, 2), camView.get(2, 2)
        ).negate()
        val dpos: Vector3f = this.getKeyboardDeltaPos(forwardDir, rightDir)
            .add(this.getGamepadDeltaPos(forwardDir, rightDir))
        if(dpos.length() > 0f) {
            this.position.add(dpos.normalize().mul(PLAYER_SPEED).mul(deltaTime))
        }
        for(bound in this.bounds) {
            this.position = bound.insideClosestTo(this.position)
        }
    }

    fun updateRotation(deltaTime: Float, inVr: Boolean) {
        // angleY is the rotation AROUND the Y axis, angleX AROUND the X axis
        this.angleY -= Mouse.dpos.x() / 3000f * TAU
        val stickRot = VrController.RIGHT.stick.x() * PLAYER_STICK_TURN_SPEED
        this.angleY -= stickRot * deltaTime
        this.angleX -= Mouse.dpos.y() / 3000f * TAU
        this.angleX = min(max(this.angleX, -89.degrees), 89.degrees)
        if(inVr) { this.angleX = 0f }
    }

    fun update(deltaTime: Float, inVr: Boolean, windowCam: Camera) {
        Mouse.cursorHidden = true
        this.updatePosition(deltaTime, windowCam)
        this.updateRotation(deltaTime, inVr)
    }

    fun configureCamera(camera: Camera) {
        camera.pos = Vector3f(this.position).add(PLAYER_CAM_OFFSET)
        camera.dir = Vector3f(NDC_INTO_SCREEN)
            .rotateX(this.angleX)
            .rotateY(this.angleY)
        camera.up = Vector3f(0f, 1f, 0f)
    }

}