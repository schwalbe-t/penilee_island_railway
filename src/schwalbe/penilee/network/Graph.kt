
package schwalbe.penilee.network

import schwalbe.penilee.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Graph(val tilesX: Int, val tilesZ: Int) {

    companion object {
        val TILE_SIZE: Float = 5f
    }

    class Signal(
        var proceed: Boolean = false
    )

    class Connection(
        val toNode: Node?,
        val toHigh: Boolean,
        var signal: Signal? = null
    )

    class Node(
        val x: Int,
        val z: Int,
        val track: Track,
        val lowTo: MutableList<Connection> = mutableListOf(),
        var currLowTo: Connection? = null,
        val highTo: MutableList<Connection> = mutableListOf(),
        var currHighTo: Connection? = null
    )

    class Track(
        val type: Type,
        val angle: Float, 
        val mirror: Boolean = false
    ) {
        enum class Type(
            val model: ObjLoader,
            val points: List<Vector3fc>
        ) {
            STRAIGHT(RAIL_STRAIGHT, listOf(
                Vector3f( 0.00f, 0f, -2.50f),
                Vector3f( 0.00f, 0f, +2.50f)
            )),
            DIAGONAL(RAIL_DIAGONAL, listOf(
                Vector3f(+2.50f, 0f, -2.50f),
                Vector3f(-2.50f, 0f, +2.50f)
            )),
            CURVE(RAIL_CURVE, listOf(
                Vector3f(+2.50f, 0f, -2.50f),
                Vector3f(+1.85f, 0f, -1.85f),
                Vector3f(+1.08f, 0f, -0.95f),
                Vector3f(+0.50f, 0f, +0.13f),
                Vector3f(+0.15f, 0f, +1.30f),
                Vector3f( 0.00f, 0f, +2.50f)
            ));
        }

        fun computeTransform(x: Int, z: Int): Matrix4f = Matrix4f()
            .translate(
                (x.toFloat() + 0.5f) * Graph.TILE_SIZE,
                0f,
                (z.toFloat() + 0.5f) * Graph.TILE_SIZE
            )
            .rotateY(this.angle)
            .scale(if(this.mirror) -1f else 1f, 1f, 1f)

        enum class Dir {
            ASCEND, DESCEND;

            companion object {
                fun ascendIf(isAscend: Boolean): Dir
                    = if(isAscend) Dir.ASCEND else Dir.DESCEND
                fun descendIf(isDescend: Boolean): Dir
                    = if(isDescend) Dir.DESCEND else Dir.ASCEND
            }

            val isAscend: Boolean
                get() = this == Dir.ASCEND
            val isDescend: Boolean
                get() = this == Dir.DESCEND
        }
    }

    val tiles: Array<MutableList<Node>> = Array(tilesX * tilesZ) { 
        mutableListOf() 
    }

    fun tileAt(x: Int, z: Int): MutableList<Node>
        = this.tiles[z * this.tilesX + x]

    val signals: MutableList<Signal> = mutableListOf()
    val inputs: MutableList<Connection> = mutableListOf()

    private var trackInstances: Map<Track.Type, MutableList<Matrix4fc>>
        = mapOf()

    fun buildTrackInstances() {
        val instances = mutableMapOf<Track.Type, MutableList<Matrix4fc>>()
        for(x in 0..<this.tilesX) {
            for(z in 0..<this.tilesZ) {
                for(node in this.tileAt(x, z)) {
                    instances
                        .getOrPut(node.track.type) { mutableListOf() }
                        .add(node.track.computeTransform(x, z))
                }
            }
        }
        this.trackInstances = instances
    }

    fun renderShadows(renderer: Renderer) {
        for((pieceType, instances) in this.trackInstances) {
            renderer.renderShadows(pieceType.model.get(), instances)
        }
    }

    fun render(renderer: Renderer) {
        for((pieceType, instances) in this.trackInstances) {
            renderer.render(pieceType.model.get(), instances)
        }
    }

}