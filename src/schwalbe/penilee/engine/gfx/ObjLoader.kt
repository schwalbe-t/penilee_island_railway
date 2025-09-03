
package schwalbe.penilee.engine.gfx

import schwalbe.penilee.engine.Resource
import org.joml.*
import java.io.File
import java.nio.file.Paths

enum class ObjAttrib(val inVertex: Pair<Int, Geometry.Type>) {
    POSITION(Pair(3, Geometry.Type.FLOAT)),
    TEX_COORDS(Pair(2, Geometry.Type.FLOAT)),
    NORMAL(Pair(3, Geometry.Type.FLOAT))
}

class ObjLoader(
    val path: String,
    val layout: List<ObjAttrib>,
    val faceCulling: FaceCulling = FaceCulling.DISABLED
): Resource<Model>() {
    override fun load(): Model {
        val meshes = mutableMapOf<Pair<String, String>, Model.Mesh>()
        loadObjModel(path, layout, meshes)
        return Model(meshes, faceCulling)
    }
}

private fun parseObjWords(line: String): List<String>
    = line.split(" ")

private fun loadObjModel(
    path: String, layout: List<ObjAttrib>, 
    meshes: MutableMap<Pair<String, String>, Model.Mesh>
) {
    val file = File(path)
    val dir: String = file.getParentFile().getAbsolutePath()
    val vertexLayout: VertexLayout = layout.map { it.inVertex }
    val materials: MutableMap<String, Texture2> = mutableMapOf()
    val positions: MutableList<Vector3f> = mutableListOf()
    val texCoords: MutableList<Vector2f> = mutableListOf()
    val normals: MutableList<Vector3f> = mutableListOf()
    var meshId: Pair<String, String> = Pair("", "")
    val meshBuilders = mutableMapOf<Pair<String, String>, Geometry.Builder>()
    for(line in file.readText().lines()) {
        val words = parseObjWords(line)
        if(words.size == 0) { continue }
        when(words[0]) {
            "mtllib" -> {
                val absPath: String = Paths.get(dir, words[1]).toString()
                loadObjMaterials(absPath, materials)
            }
            "o" -> {
                meshId = Pair(words[1], meshId.second)
            }
            "usemtl" -> {
                meshId = Pair(meshId.first, words[1])
            }
            "v" -> {
                val v = Vector3f(
                    words[1].toDouble().toFloat(),
                    words[2].toDouble().toFloat(),
                    words[3].toDouble().toFloat()
                )
                if(words.size >= 5) {
                    v.div(words[4].toDouble().toFloat())
                }
                positions.add(v)
            }
            "vt" -> {
                val vt = Vector2f(words[1].toDouble().toFloat(), 0f)
                if(words.size >= 3) { 
                    vt.y = words[2].toDouble().toFloat() 
                }
                texCoords.add(vt)
            }
            "vn" -> {
                val vn = Vector3f(
                    words[1].toDouble().toFloat(),
                    words[2].toDouble().toFloat(),
                    words[3].toDouble().toFloat()
                )
                vn.normalize()
                normals.add(vn)
            }
            "f" -> {
                val builder = meshBuilders
                    .getOrPut(meshId) { Geometry.Builder(vertexLayout) }
                // TODO! parse IDs and build vertex
            }
        }
    }
    // TODO! build geometries on GPU and build and insert mesh objects
}

private fun loadObjMaterials(
    path: String, textures: MutableMap<String, Texture2>
) {
    val file = File(path)
    val dir: String = file.getParentFile().getAbsolutePath()
    var materialName: String = ""
    for(line in file.readText().lines()) {
        val words = parseObjWords(line)
        if(words.size == 0) { continue }
        when(words[0]) {
            "newmtl" -> { 
                materialName = words[1] 
            }
            "map_Kd" -> {
                val absPath: String = Paths.get(dir, words[1]).toString()
                val loader = ImageLoader(absPath)
                loader.cachedLoad()
                textures.put(materialName, loader.get())
            }
        }
    }
}