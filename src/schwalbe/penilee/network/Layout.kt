
package schwalbe.penilee.network

import schwalbe.penilee.*
import schwalbe.penilee.engine.*
import kotlin.math.max
import org.joml.*

class Layout(
    val tracks: Tracks
) {

    class LeverHandler(
        val type: Type,
        val isLocked: () -> Boolean, 
        val onChange: (Boolean) -> Unit
    ) {
        enum class Type { SIGNAL, POINT, CROSSING }
    }

    fun createLevers(): List<LeverHandler> {
        // TODO!
        return listOf(
            LeverHandler(LeverHandler.Type.SIGNAL, { false }, { state -> }),
            LeverHandler(LeverHandler.Type.POINT, { false }, { state -> }),
            LeverHandler(LeverHandler.Type.CROSSING, { false }, { state -> })
        )
    }

    class EbiHandler(
        val isClear: () -> Boolean
    )

    fun createEbis(): List<EbiHandler> {
        // TODO!
        return listOf()
    }

    class BellHandler(
        val onPress: () -> Unit,
        val getOnRing: () -> () -> Unit
    )

    fun createBells(): List<BellHandler> {
        // TODO!
        return listOf()
    }

    fun update(deltaTime: Float) {
        // TODO!
    }

    fun renderShadows(renderer: Renderer) {
        this.tracks.renderShadows(renderer)
    }

    fun render(renderer: Renderer) {
        this.tracks.render(renderer)
    }

}


    // ""      (length) => 0
    // "#"     (length) => 1
    // "# "    (length) => 1
    // "# #"   (length) => 2
    // "# # "  (length) => 2
    // "# # #" (length) => 3
    // ...
fun textLayoutExtent(textExtent: Int): Int
    = (textExtent + 1) / 2

val WILDCARD_TRACK_RULES: List<Pair<List<String>, Tracks.Piece>> = listOf(
    listOf(
        "X  ",
        " * ",
        "  X"
    ) to Tracks.Piece(Tracks.Piece.Type.DIAGONAL, 90.degrees),
    listOf(
        "  X",
        " * ",
        "X  "
    ) to Tracks.Piece(Tracks.Piece.Type.DIAGONAL, 0.degrees),
    listOf(
        "   ",
        "-*-",
        "   "
    ) to Tracks.Piece(Tracks.Piece.Type.STRAIGHT, 90.degrees),
    listOf(
        " | ",
        " * ",
        " | "
    ) to Tracks.Piece(Tracks.Piece.Type.STRAIGHT, 0.degrees),
    listOf(
        "  X",
        " * ",
        " | "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 0.degrees, mirror = false),
    listOf(
        "X  ",
        " *-",
        "   "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 90.degrees, mirror = false),
    listOf(
        " | ",
        " * ",
        "X  "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 180.degrees, mirror = false),
    listOf(
        "   ",
        "-* ",
        "  X"
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 270.degrees, mirror = false),
    listOf(
        "X  ",
        " * ",
        " | "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 0.degrees, mirror = true),
    listOf(
        "   ",
        " *-",
        "X  "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 90.degrees, mirror = true),
    listOf(
        " | ",
        " * ",
        "  X"
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 180.degrees, mirror = true),
    listOf(
        "  X",
        "-* ",
        "   "
    ) to Tracks.Piece(Tracks.Piece.Type.CURVE, 270.degrees, mirror = true)
)

fun matchesWildcardTrackRule(
    tx: Int, tz: Int, lines: List<String>,
    rule: Pair<List<String>, Tracks.Piece>
): Boolean {
    for(oz in -1..+1) {
        val z: Int = tz * 2 + oz
        val zInBounds: Boolean = z >= 0 && z < lines.size
        val cmpZ: String = rule.first[1 + oz]
        for(ox in -1..+1) {
            val x: Int = tx * 2 + ox
            val inBounds: Boolean = zInBounds && x >= 0 && x < lines[z].length
            val c: Char = if(inBounds) lines[z][x] else ' '
            val cmp: Char = cmpZ[1 + ox]
            val matches: Boolean = when(cmp) {
                ' ' -> true
                'X' -> when(c) {
                    'X', '/', '\\' -> true
                    else -> false
                }
                else -> c == cmp
            }
            if(!matches) { return false }
        }
    }
    return true
}

fun parseTrackLayout(file: String): Layout {
    val lines: List<String> = file.lines()
    val tilesX: Int = lines.map { textLayoutExtent(it.length) }.reduce(::max)
    val tilesZ: Int = textLayoutExtent(lines.size)
    val tracks = Tracks(tilesX, tilesZ)
    for(z in 0..<tilesZ) {
        val line: String = lines[z * 2]
        val lineTilesX: Int = textLayoutExtent(line.length)
        for(x in 0..<lineTilesX) {
            val tile: Tracks.Tile = tracks.tileAt(x, z)
            val c: Char = line[x * 2]
            when(c) {
                '-' -> tile.pieces.add(Tracks.Piece(
                    Tracks.Piece.Type.STRAIGHT, 90.degrees
                ))
                '/' -> tile.pieces.add(Tracks.Piece(
                    Tracks.Piece.Type.DIAGONAL, 0.degrees
                ))
                '|' -> tile.pieces.add(Tracks.Piece(
                    Tracks.Piece.Type.STRAIGHT, 0.degrees
                ))
                '\\' -> tile.pieces.add(Tracks.Piece(
                    Tracks.Piece.Type.DIAGONAL, 90.degrees
                ))
                '*' -> {
                    WILDCARD_TRACK_RULES
                        .filter { matchesWildcardTrackRule(x, z, lines, it) }
                        .forEach { tile.pieces.add(it.second) }
                }
                else -> {}
            }
        }
    }
    tracks.buildInstances()
    return Layout(tracks)
}