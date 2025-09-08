
package schwalbe.penilee.engine.gfx

import org.joml.*
import kotlin.math.*
import schwalbe.penilee.engine.*

class Camera {

    var pos: Vector3f = Vector3f()
    var dir: Vector3f = Vector3f()
    var up: Vector3f = Vector3f()
    var fovLeft: Float = 0f
    var fovRight: Float = 0f
    var fovDown: Float = 0f
    var fovUp: Float = 0f
    var nearPlane: Float = 0.1f
    var farPlane: Float = 500.0f

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

    fun computePos(child: Vector3f = Vector3f()): Vector3f {
        val p: Vector3f = child.add(this.pos)
        this.parent?.computePos(p)
        return p
    }

    fun computeView(child: Matrix4f = Matrix4f()): Matrix4f {
        val center = Vector3f()
        val nUp = Vector3f()
        this.pos.add(this.dir, center)
        this.up.normalize(nUp)
        child.lookAt(this.pos, center, nUp)
        this.parent?.computeView(child)
        return child
    }

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

    fun worldToView(worldPos: Vector3fc): Vector3f
        = this.computeView()
            .transformPosition(Vector3f(worldPos))

    fun viewToNdc(viewPos: Vector3fc): Vector3f
        = this.computeProj()
            .transformProject(Vector3f(viewPos))

}