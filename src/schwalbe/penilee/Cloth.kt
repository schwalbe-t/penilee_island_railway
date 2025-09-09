
package schwalbe.penilee

import schwalbe.penilee.engine.gfx.*
import kotlin.math.*
import org.joml.*

class Cloth(
    origin: Vector3fc, dirX: Vector3fc, dirY: Vector3fc,
    val width: Int, val height: Int, nodeDist: Float,
    var texture: Texture2, var stiffness: Float, var mass: Float
) {

    companion object {
        val GRAVITY: Vector3fc = Vector3f(0f, -1f, 0f) // units/s^2
        val DAMPING: Float = 0.999f // fraction removed from velocity / second
        internal val LOCAL_TRANSF: Matrix4fc = Matrix4f()
        internal val MODEL_TRANSF: List<Matrix4fc> = listOf(Matrix4f())
        internal val MAX_TIMESTEP: Float = 0.001f
        internal val SHEAR_STIFFNESS: Float = 1f // relative to cloth stiffness
    }


    val nodes: Array<Node> = Array((this.width + 1) * (this.height + 1)) { 
        Node(listOf()) 
    }

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
            }
        }
    }

    class Spring(val node: Node, val dist: Float, val stiffness: Float = 1f)

    class Node(var connected: List<Spring>) {

        var fixedAt: Vector3fc? = null

        val position: Vector3f = Vector3f()
        val velocity: Vector3f = Vector3f()
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

        fun updatePosition(deltaTime: Float, mass: Float) {
            if(this.fixedAt != null) {
                this.position.set(this.fixedAt)
                this.velocity.set(0f, 0f, 0f)
                return
            }
            this.velocity.add(Vector3f(this.force).div(mass).mul(deltaTime))
            this.velocity.mul(1f - Cloth.DAMPING * deltaTime)
            this.position.add(Vector3f(this.velocity).mul(deltaTime))
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

    private fun advanceNodes(deltaTime: Float) {
        this.nodes.forEach { it.updateForce(this.stiffness) }
        this.nodes.forEach { it.updatePosition(deltaTime, this.mass) }
    }

    fun update(deltaTime: Float) {
        var remTime: Float = deltaTime
        while(remTime > 0f) {
            val step: Float = min(Cloth.MAX_TIMESTEP, remTime)
            this.advanceNodes(step)
            remTime -= step
        }
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