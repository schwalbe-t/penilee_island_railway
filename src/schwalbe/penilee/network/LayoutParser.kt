
package schwalbe.penilee.network.parser

import schwalbe.penilee.network.*
import schwalbe.penilee.engine.*
import kotlin.math.max
import org.joml.*

    // ""      (length) => 0
    // "#"     (length) => 1
    // "# "    (length) => 1
    // "# #"   (length) => 2
    // "# # "  (length) => 2
    // "# # #" (length) => 3
    // ...
fun textLayoutExtent(textExtent: Int): Int
    = (textExtent + 1) / 2

val WILDCARD_TRACK_RULES: List<Pair<List<String>, Graph.Track>> = listOf(
    listOf(
        "X  ",
        " * ",
        "  X"
    ) to Graph.Track(Graph.Track.Type.DIAGONAL, 90.degrees),
    listOf(
        "  X",
        " * ",
        "X  "
    ) to Graph.Track(Graph.Track.Type.DIAGONAL, 0.degrees),
    listOf(
        "   ",
        "-*-",
        "   "
    ) to Graph.Track(Graph.Track.Type.STRAIGHT, 90.degrees),
    listOf(
        " | ",
        " * ",
        " | "
    ) to Graph.Track(Graph.Track.Type.STRAIGHT, 0.degrees),
    listOf(
        "  X",
        " * ",
        " | "
    ) to Graph.Track(Graph.Track.Type.CURVE, 0.degrees, mirror = false),
    listOf(
        "X  ",
        " *-",
        "   "
    ) to Graph.Track(Graph.Track.Type.CURVE, 90.degrees, mirror = false),
    listOf(
        " | ",
        " * ",
        "X  "
    ) to Graph.Track(Graph.Track.Type.CURVE, 180.degrees, mirror = false),
    listOf(
        "   ",
        "-* ",
        "  X"
    ) to Graph.Track(Graph.Track.Type.CURVE, 270.degrees, mirror = false),
    listOf(
        "X  ",
        " * ",
        " | "
    ) to Graph.Track(Graph.Track.Type.CURVE, 0.degrees, mirror = true),
    listOf(
        "   ",
        " *-",
        "X  "
    ) to Graph.Track(Graph.Track.Type.CURVE, 90.degrees, mirror = true),
    listOf(
        " | ",
        " * ",
        "  X"
    ) to Graph.Track(Graph.Track.Type.CURVE, 180.degrees, mirror = true),
    listOf(
        "  X",
        "-* ",
        "   "
    ) to Graph.Track(Graph.Track.Type.CURVE, 270.degrees, mirror = true)
)

fun matchesWildcardTrackRule(
    tx: Int, tz: Int, lines: List<String>,
    rule: Pair<List<String>, Graph.Track>
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

fun parseTileTrackPieces(
    x: Int, z: Int, lines: List<String>, c: Char
): List<Graph.Track> = when(c) {
    '-' -> listOf(Graph.Track(
        Graph.Track.Type.STRAIGHT, 90.degrees
    ))
    '/' -> listOf(Graph.Track(
        Graph.Track.Type.DIAGONAL, 0.degrees
    ))
    '|' -> listOf(Graph.Track(
        Graph.Track.Type.STRAIGHT, 0.degrees
    ))
    '\\' -> listOf(Graph.Track(
        Graph.Track.Type.DIAGONAL, 90.degrees
    ))
    '*' -> WILDCARD_TRACK_RULES
        .filter { matchesWildcardTrackRule(x, z, lines, it) }
        .map { it.second }
    else -> listOf()
}

fun determinePieceConnection(
    aT: Graph.Track, aX: Int, aZ: Int,
    bT: Graph.Track, bX: Int, bZ: Int
): Pair<Boolean, Boolean>? {
    val aTf: Matrix4f = aT.computeTransform(aX, aZ)
    val aLo: Vector3f = aTf.transformPosition(Vector3f(aT.type.points.first()))
    val aHi: Vector3f = aTf.transformPosition(Vector3f(aT.type.points.last()))
    val bTf: Matrix4f = bT.computeTransform(bX, bZ)
    val bLo: Vector3f = bTf.transformPosition(Vector3f(bT.type.points.first()))
    val bHi: Vector3f = bTf.transformPosition(Vector3f(bT.type.points.last()))
    val maxD: Float = 0.01f
    if(aLo.distance(bLo) <= maxD) { return Pair(false, false) }
    if(aHi.distance(bLo) <= maxD) { return Pair(true,  false) }
    if(aLo.distance(bHi) <= maxD) { return Pair(false, true ) }
    if(aHi.distance(bHi) <= maxD) { return Pair(true,  true ) }
    return null
}

fun createSignal(
    node: Graph.Node, dir: Graph.Track.Dir,
    levers: MutableList<Layout.LeverHandler>
): Graph.Signal {
    val signal = Graph.Signal()
    levers.add(Layout.LeverHandler(
        type = Layout.LeverHandler.Type.SIGNAL,
        isLocked = {
            // TODO! interlocking checks
            null
        },
        onChange = { newState ->
            signal.proceed = newState
        }
    ))
    return signal
}

fun attemptTilePieceConnections(
    fromX: Int, fromZ: Int, toX: Int, toZ: Int, hasSignal: Boolean,
    graph: Graph, levers: MutableList<Layout.LeverHandler>
) { 
    for(fromNode in graph.tileAt(fromX, fromZ)) {
        var fromDir: Graph.Track.Dir? = null
        for(toNode in graph.tileAt(toX, toZ)) {
            val conn: Pair<Boolean, Boolean>? = determinePieceConnection(
                fromNode.track, fromX, fromZ, toNode.track, toX, toZ
            )
            if(conn == null) { continue }
            val connFromDir = Graph.Track.Dir.ascendIf(conn.first) 
            if(fromDir == null) { fromDir = connFromDir }
            if(fromDir != connFromDir) {
                throw IllegalArgumentException(
                    "Piece connection may only affect one side at a time"
                )
            }
            val fromConnects = if(fromDir.isAscend) fromNode.highTo 
                else fromNode.lowTo
            fromConnects.add(Graph.Connection(toNode, conn.second))
        }
        if(fromDir == null || !hasSignal) { continue }
        val fromConnects = if(fromDir.isAscend) fromNode.highTo 
            else fromNode.lowTo
        val signal: Graph.Signal = createSignal(fromNode, fromDir, levers)
        fromConnects.forEach { it.signal = signal }
    }
}

fun getGridCharAt(lines: List<String>, x: Int, z: Int, dflt: Char = ' '): Char {
    if(z < 0 || z >= lines.size) { return dflt }
    val line: String = lines[z]
    if(x < 0 || x >= line.length) { return dflt }
    return line[x]
}

fun parseTileConnection(
    cX: Int, cZ: Int, lines: List<String>, 
    graph: Graph, 
    levers: MutableList<Layout.LeverHandler>,
    ebis: MutableList<Layout.EbiHandler>,
    bells: MutableList<Layout.BellHandler>
) {
    val x: Int = cX / 2
    val z: Int = cZ / 2
    when(lines[cZ][cX]) {
        '-' -> {
            val hasSigR: Boolean = getGridCharAt(lines, cX, cZ+1) == 'F'
            val hasSigL: Boolean = getGridCharAt(lines, cX, cZ-1) == 'F'
            attemptTilePieceConnections(x, z, x+1, z, hasSigR, graph, levers)
            attemptTilePieceConnections(x+1, z, x, z, hasSigL, graph, levers)
        }
        '/' -> {
            val hasSigTR: Boolean = getGridCharAt(lines, cX+1, cZ) == 'F'
            val hasSigBL: Boolean = getGridCharAt(lines, cX-1, cZ) == 'F'
            attemptTilePieceConnections(x+1, z, x, z+1, hasSigBL, graph, levers)
            attemptTilePieceConnections(x, z+1, x+1, z, hasSigTR, graph, levers)
        }
        '|' -> {
            val hasSigT: Boolean = getGridCharAt(lines, cX+1, cZ) == 'F'
            val hasSigB: Boolean = getGridCharAt(lines, cX-1, cZ) == 'F'
            attemptTilePieceConnections(x, z, x, z+1, hasSigB, graph, levers)
            attemptTilePieceConnections(x, z+1, x, z, hasSigT, graph, levers)
        }
        '\\' -> {
            val hasSigTL: Boolean = getGridCharAt(lines, cX+1, cZ) == 'F'
            val hasSigBR: Boolean = getGridCharAt(lines, cX-1, cZ) == 'F'
            attemptTilePieceConnections(x, z, x+1, z+1, hasSigBR, graph, levers)
            attemptTilePieceConnections(x+1, z+1, x, z, hasSigTL, graph, levers)
        }
        'X' -> {
            attemptTilePieceConnections(x+1, z, x, z+1, false, graph, levers)
            attemptTilePieceConnections(x, z+1, x+1, z, false, graph, levers)
            attemptTilePieceConnections(x, z, x+1, z+1, false, graph, levers)
            attemptTilePieceConnections(x+1, z+1, x, z, false, graph, levers)
        }

        'O' -> {
            // TODO! generate corresponding EBI and bell
            // NOTE: needs to connect to '-', '/', '|' and '\'
        }
        '0' -> {
            // TODO! generate corresponding bell
            // NOTE: needs to connect to '-', '/', '|' and '\'
        }
    }
}

fun createPoint(
    node: Graph.Node, dir: Graph.Track.Dir,
    levers: MutableList<Layout.LeverHandler>
) {
    val conn = if(dir.isDescend) node.lowTo else node.highTo
    val baseIdx: Int = conn.indexOfFirst {
        val connNode: Graph.Node? = it.toNode
        if(connNode == null) false else connNode.track.type == node.track.type
    }
    val offIdx: Int = if(baseIdx != -1) baseIdx else 0
    val onIdx: Int = (offIdx + 1) % conn.size
    val lever = Layout.LeverHandler(
        type = Layout.LeverHandler.Type.POINT,
        isLocked = {
            // TODO! interlocking checks
            null
        },
        onChange = { newState ->
            val to: Graph.Connection = conn[if(newState) onIdx else offIdx]
            when(dir.isAscend) {
                false -> node.currLowTo = to
                true -> node.currHighTo = to
            }
        }
    )
    lever.onChange(false)
    levers.add(lever)
}

fun collectPoints(graph: Graph, levers: MutableList<Layout.LeverHandler>) {
    for(x in 0..<graph.tilesX) {
        for(z in 0..<graph.tilesZ) {
            for(node in graph.tileAt(x, z)) {
                when(node.lowTo.size) {
                    0 -> {}
                    1 -> node.currLowTo = node.lowTo[0]
                    2 -> createPoint(node, Graph.Track.Dir.DESCEND, levers)
                    else -> throw IllegalArgumentException(
                        "Points may only have two branches"
                    )
                }
                when(node.highTo.size) {
                    0 -> {}
                    1 -> node.currHighTo = node.highTo[0]
                    2 -> createPoint(node, Graph.Track.Dir.ASCEND, levers)
                    else -> throw IllegalArgumentException(
                        "Points may only have two branches"
                    )
                }
            }
        }
    }
}

fun parseTrackLayout(file: String): Layout {
    val lines: List<String> = file.lines()
    val tilesX: Int = lines.map { textLayoutExtent(it.length) }.reduce(::max)
    val tilesZ: Int = textLayoutExtent(lines.size)
    val graph = Graph(tilesX, tilesZ)
    val levers: MutableList<Layout.LeverHandler> = mutableListOf()
    val ebis: MutableList<Layout.EbiHandler> = mutableListOf()
    val bells: MutableList<Layout.BellHandler> = mutableListOf()
    for(z in 0..<tilesZ) {
        val line: String = lines[z * 2]
        val lineTilesX: Int = textLayoutExtent(line.length)
        for(x in 0..<lineTilesX) {
            val c: Char = line[x * 2]
            val tileNodes: List<Graph.Node> 
                = parseTileTrackPieces(x, z, lines, c)
                .map { Graph.Node(x, z, it) }
            graph.tileAt(x, z).addAll(tileNodes)
        }
    }
    for(cZ in 0..<lines.size) {
        val line: String = lines[cZ]
        for(cX in 0..<line.length) {
            if(cX % 2 == 0 && cZ % 2 == 0) { continue }
            parseTileConnection(cX, cZ, lines, graph, levers, ebis, bells)
        }
    }
    collectPoints(graph, levers)
    // TODO! collect crossings
    graph.buildTrackInstances()
    return Layout(graph, levers, ebis, bells)
}