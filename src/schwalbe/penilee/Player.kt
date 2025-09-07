
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.Camera
import schwalbe.penilee.engine.input.*
import org.joml.*
import kotlin.math.*

val PLAYER_SPEED: Float = 3f
val PLAYER_CAM_OFFSET: Vector3fc = Vector3f(0f, 1.5f, 0f)

class Player(var position: Vector3f, val bounds: List<AABB>) {

    var angleX: Float = 0f
    var angleY: Float = 0f

    fun update(deltaTime: Float, inVr: Boolean) {
        Mouse.cursorHidden = true
        val step: Vector3fc = Vector3f(NDC_INTO_SCREEN).rotateY(this.angleY)
        val dpos = Vector3f()
        if(Key.W.isPressed) { dpos.add(step.x(), 0f, step.z()) }
        if(Key.A.isPressed) { dpos.add(step.z(), 0f, -step.x()) }
        if(Key.S.isPressed) { dpos.add(-step.x(), 0f, -step.z()) }
        if(Key.D.isPressed) { dpos.add(-step.z(), 0f, step.x()) }
        if(dpos.length() > 0f) {
            this.position.add(dpos.normalize().mul(PLAYER_SPEED).mul(deltaTime))
        }
        for(bound in this.bounds) {
            this.position = bound.insideClosestTo(this.position)
        }
        // angleY is the rotation AROUND the Y axis, angleX AROUND the X axis
        this.angleY -= Mouse.dpos.x() / 3000f * TAU
        this.angleX -= Mouse.dpos.y() / 3000f * TAU
        this.angleX = min(max(this.angleX, -89.degrees), 89.degrees)
        if(inVr) { this.angleX = 0f }
    }

    fun configureCamera(camera: Camera) {
        camera.pos = Vector3f(this.position).add(PLAYER_CAM_OFFSET)
        camera.dir = Vector3f(NDC_INTO_SCREEN)
            .rotateX(this.angleX)
            .rotateY(this.angleY)
        camera.up = Vector3f(0f, 1f, 0f)
    }

}