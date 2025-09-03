
package schwalbe.penilee.engine.gfx

import org.joml.*
import kotlin.math.*

class Camera {

    var pos: Vector3f = Vector3f()
    var dir: Vector3f = Vector3f()
    var up: Vector3f = Vector3f()
    var fovLeft: Float = 0f
    var fovRight: Float = 0f
    var fovDown: Float = 0f
    var fovUp: Float = 0f
    var nearPlane: Float = 0.1f
    var farPlane: Float = 1000.0f

    var parent: Camera? = null

    constructor(
        pos: Vector3f = Vector3f(0f, 0f, 0f), 
        dir: Vector3f = Vector3f(0f, 0f, -1f),
        up: Vector3f = Vector3f(0f, 1f, 0f)
    ) {
        this.pos = pos
        this.dir = dir
        this.up = up
    }

    fun setHorizontalFov(hFov: Float, aspectRatio: Float) {
        val hHalf: Float = hFov / 2.0f;
        val vHalf: Float = atan(tan(hHalf) / aspectRatio);
        this.fovLeft = -hHalf
        this.fovRight = hHalf
        this.fovDown = -vHalf
        this.fovUp = vHalf
    }

    fun computeRotation(): Matrix4f {
        val pRot: Matrix4f = this.parent?.computeRotation() ?: Matrix4f()
        val rot: Matrix4f = Matrix4f().lookAlong(this.dir, this.up)
        return rot.mul(pRot)
    }

    fun computeTranslation(): Matrix4f {
        val transl: Matrix4f = this.parent?.computeTranslation() ?: Matrix4f()
        transl.translate(-this.pos.x(), -this.pos.y(), -this.pos.z())
        return transl
    }

    fun computeView(): Matrix4f 
        = this.computeRotation().mul(this.computeTranslation())

    fun computeProj(): Matrix4f {
        val r = Matrix4f()
        r.perspectiveOffCenterFov(
            this.fovLeft, this.fovRight, this.fovDown, this.fovUp,
            this.nearPlane, this.farPlane
        )
        return r
    }

    fun computeViewProj(): Matrix4f
        = this.computeProj().mul(this.computeView())

}