
package schwalbe.penilee.network

import schwalbe.penilee.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Tracks(val tilesX: Int, val tilesZ: Int) {

    companion object {
        val TILE_SIZE: Float = 5f
    }

    class Piece(val type: Type, val angle: Float, val mirror: Boolean = false) {
        enum class Type(val model: ObjLoader) {
            STRAIGHT(RAIL_STRAIGHT),
            DIAGONAL(RAIL_DIAGONAL),
            CURVE(RAIL_CURVE);
        }
    }

    class Tile(
        var pieces: MutableList<Tracks.Piece> = mutableListOf()
    )

    val tiles: Array<Tile> = Array(tilesX * tilesZ) { Tile() }

    fun tileAt(x: Int, z: Int): Tile
        = this.tiles[z * this.tilesX + x]

    private var instances: Map<Piece.Type, MutableList<Matrix4fc>>
        = mapOf()

    fun buildInstances() {
        val instances = mutableMapOf<Piece.Type, MutableList<Matrix4fc>>()
        for(x in 0..<this.tilesX) {
            for(z in 0..<this.tilesZ) {
                val tile: Tile = this.tileAt(x, z)
                for(piece in tile.pieces) {
                    val pos: Vector3f = Vector3f(x.toFloat(), 0f, z.toFloat())
                        .mul(Tracks.TILE_SIZE)
                    val instance: Matrix4fc = Matrix4f()
                        .translate(pos)
                        .rotateY(piece.angle)
                        .scale(if(piece.mirror) -1f else 1f, 1f, 1f)
                    val tInstances: MutableList<Matrix4fc> = instances
                        .getOrPut(piece.type) { mutableListOf() }
                    tInstances.add(instance)
                }
            }
        }
        this.instances = instances
    }

    fun renderShadows(renderer: Renderer) {
        for((pieceType, instances) in this.instances) {
            renderer.renderShadows(pieceType.model.get(), instances)
        }
    }

    fun render(renderer: Renderer) {
        for((pieceType, instances) in this.instances) {
            renderer.render(pieceType.model.get(), instances)
        }
    }

}