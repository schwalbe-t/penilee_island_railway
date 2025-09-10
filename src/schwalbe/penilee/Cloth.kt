
package schwalbe.penilee

import schwalbe.penilee.engine.gfx.*
import kotlin.math.*
import org.joml.*

class Cloth(
    origin: Vector3fc, dirX: Vector3fc, dirY: Vector3fc,
    val width: Int, val height: Int, nodeDist: Float,
    var texture: Texture2, 
    var stiffness: Float, var mass: Float, var damping: Float,
    var colliders: List<Collider>
) {

    companion object {
        val GRAVITY: Vector3fc = Vector3f(0f, -10f, 0f) // units/s^2
        internal val LOCAL_TRANSF: Matrix4fc = Matrix4f()
        internal val MODEL_TRANSF: List<Matrix4fc> = listOf(Matrix4f())
        internal val TIMESTEP: Float = 1f / 500f
        internal val SHEAR_STIFFNESS: Float = 1f // relative to cloth stiffness
        internal val MAX_COLLIDER_CORR: Float = 0.001f
    }
    
    class Collider(var center: Vector3f, var radius: Float) {

        private var prevCenter: Vector3f = Vector3f(center)
        private var lerpCenter: Vector3f = Vector3f(center)

        fun update(frameProg: Float) {
            this.lerpCenter.set(this.center)
                .sub(this.prevCenter)
                .mul(frameProg)
                .add(this.prevCenter)
        }

        fun endFrame() {
            this.prevCenter.set(this.center)
        }

        fun apply(point: Vector3fc): Vector3f {
            val toPoint: Vector3fc = Vector3f(point).sub(this.lerpCenter)
            val dist: Float = toPoint.length()
            if(dist == 0f || dist >= this.radius) { return Vector3f(point) }
            val newPos: Vector3f = Vector3f(toPoint)
                .normalize()
                .mul(this.radius)
                .add(this.lerpCenter)
            val correction: Vector3f = Vector3f(newPos).sub(point)
            val correctionL: Float = correction.length()
            if(correctionL > Cloth.MAX_COLLIDER_CORR) {
                correction.div(correctionL).mul(Cloth.MAX_COLLIDER_CORR)
            }
            return Vector3f(point).add(correction)
        }

    }


    val nodes: Array<Node> = Array((this.width + 1) * (this.height + 1)) { 
        Node(listOf()) 
    }

    private var remDeltaTime: Float = 0f
    private val nodeIndices: Array<UShort> = Array(this.nodes.size) { 0u }
    private var geometry: Geometry? = null


    fun nodeIdxAt(x: Int, y: Int): Int
        = y * (this.width + 1) + x

    fun nodeAt(x: Int, y: Int): Node
        = this.nodes[this.nodeIdxAt(x, y)]

    init {
        val sqrt2: Float = sqrt(2f)
        for(x in 0..this.width) {
            for(y in 0..this.height) {
                val node = this.nodeAt(x, y)
                val c = mutableListOf<Spring>()
                val hasLeft: Boolean = x > 0
                val hasRight: Boolean = x < this.width
                val hasTop: Boolean = y > 0
                val hasBottom: Boolean = y < this.height
                val d: Float = nodeDist
                val dd: Float = sqrt2 * d
                if(hasLeft) { c.add(Spring(this.nodeAt(x - 1, y), d)) }
                if(hasRight) { c.add(Spring(this.nodeAt(x + 1, y), d)) }
                if(hasTop) { c.add(Spring(this.nodeAt(x, y - 1), d)) }
                if(hasBottom) { c.add(Spring(this.nodeAt(x, y + 1), d)) }
                if(hasLeft && hasTop) {
                    c.add(Spring(
                        this.nodeAt(x - 1, y - 1), dd, Cloth.SHEAR_STIFFNESS
                    ))
                }
                if(hasRight && hasBottom) {
                    c.add(Spring(
                        this.nodeAt(x + 1, y + 1), dd, Cloth.SHEAR_STIFFNESS
                    ))
                }
                node.connected = c
                node.position.set(origin)
                node.position.add(Vector3f(dirX).mul(x.toFloat() * nodeDist))
                node.position.sub(Vector3f(dirY).mul(y.toFloat() * nodeDist))
                node.prevPosition.set(node.position)
            }
        }
    }

    class Spring(val node: Node, val dist: Float, val stiffness: Float = 1f)

    class Node(var connected: List<Spring>) {

        private var prevFixedAt: Vector3fc? = null
        var fixedAt: Vector3fc? = null

        internal val prevPosition: Vector3f = Vector3f()
        val position: Vector3f = Vector3f()
        val force: Vector3f = Vector3f()

        fun updateForce(stiffness: Float) {
            if(this.fixedAt != null) {
                this.force.set(0f, 0f, 0f)
                return
            }
            this.force.set(Cloth.GRAVITY)
            for(spring in this.connected) {
                val toNeighbor = Vector3f(spring.node.position)
                    .sub(this.position)
                val dist: Float = toNeighbor.length()
                if(dist == 0f) { continue }
                val force: Vector3f = Vector3f(toNeighbor).div(dist)
                    .mul((dist - spring.dist) * stiffness * spring.stiffness)
                this.force.add(force)
            }
        }

        fun updatePosition(mass: Float, damping: Float, frameProg: Float) {
            val fixedAt: Vector3fc? = this.fixedAt
            if(fixedAt != null) {
                val prevFixedAt: Vector3fc = this.prevFixedAt ?: fixedAt
                this.prevPosition.set(this.position)
                this.position.set(fixedAt)
                    .sub(prevFixedAt)
                    .mul(frameProg)
                    .add(prevFixedAt)
                return
            }
            this.prevFixedAt = null
            val velocity: Vector3f = Vector3f(this.position)
                .sub(this.prevPosition)
                .mul(1f - damping)
            val acceleration: Vector3f = Vector3f(this.force)
                .div(mass)
                .mul(Cloth.TIMESTEP.pow(2))
            this.prevPosition.set(this.position)
            this.position.add(velocity).add(acceleration)
        }

        fun endFrame() {
            val fixedAt: Vector3fc? = this.fixedAt
            if(fixedAt == null) { return }
            this.prevFixedAt = Vector3f(fixedAt)
        }

    }

    private fun buildVertex(gb: Geometry.Builder, x: Int, y: Int): UShort {
        val n: Node = this.nodeAt(x, y)
        val tc_u: Float = x.toFloat() / this.width.toFloat()
        val tc_v: Float = 1f - (y.toFloat() / this.height.toFloat())
        return gb.putVertex { it
            .putFloats(n.position.x(), n.position.y(), n.position.z())
            .putFloats(tc_u, tc_v)
            .putFloats(0f, 0f, 0f)
        }
    }

    private fun buildGeometry(): Geometry {
        val gb = Geometry.Builder(Renderer.VERTEX_LAYOUT)
        for(x in 0..this.width) {
            for(y in 0..this.height) {
                val nodeIdx: Int = this.nodeIdxAt(x, y)
                this.nodeIndices[nodeIdx] = this.buildVertex(gb, x, y)
            }
        }
        for(x in 0..<this.width) {
            for(y in 0..<this.height) {
                val tl = this.nodeIndices[this.nodeIdxAt(x, y)]
                val tr = this.nodeIndices[this.nodeIdxAt(x + 1, y)]
                val bl = this.nodeIndices[this.nodeIdxAt(x, y + 1)]
                val br = this.nodeIndices[this.nodeIdxAt(x + 1, y + 1)]
                gb.putElement(tl, bl, br)
                gb.putElement(tl, br, tr)
            }
        }
        return gb.build()
    }

    private fun advanceNodes(frameProg: Float) {
        this.nodes.forEach { n -> n.updateForce(this.stiffness) }
        this.colliders.forEach { c -> c.update(frameProg) }
        this.nodes.forEach { n ->
            n.updatePosition(this.mass, this.damping, frameProg) 
            if(n.fixedAt == null) {
                this.colliders.forEach { c ->
                    n.position.set(c.apply(n.position))
                }
            }
        }
    }

    fun update(deltaTime: Float) {
        this.remDeltaTime += deltaTime
        val timeSteps: Int = (this.remDeltaTime / Cloth.TIMESTEP).toInt()
        for(ts in 1..timeSteps) {
            val frameProg: Float = ts.toFloat() / timeSteps.toFloat()
            this.advanceNodes(frameProg)
        }
        this.remDeltaTime -= timeSteps * Cloth.TIMESTEP
        this.nodes.forEach(Node::endFrame)
        this.colliders.forEach(Collider::endFrame)
        this.geometry?.destroy()
        this.geometry = this.buildGeometry()
    }

    fun renderShadows(renderer: Renderer) {
        val geometry: Geometry? = this.geometry
        if(geometry == null) { return }
        renderer.renderShadows(
            geometry, this.texture, Cloth.LOCAL_TRANSF, Cloth.MODEL_TRANSF,
            FaceCulling.DISABLED 
        )
    }

    fun render(renderer: Renderer) {
        val geometry: Geometry? = this.geometry
        if(geometry == null) { return }
        renderer.render(
            geometry, this.texture, Cloth.LOCAL_TRANSF, Cloth.MODEL_TRANSF,
            FaceCulling.DISABLED, DepthTesting.ENABLED
        )
    }

    fun destroy() {
        this.geometry?.destroy()
    }

}