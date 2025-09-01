
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

    fun computeProjection(): Matrix4f {
        val r = Matrix4f()
        r.perspectiveOffCenterFov(
            this.fovLeft, this.fovRight, this.fovDown, this.fovUp,
            this.nearPlane, this.farPlane
        )
        return r
    }

}