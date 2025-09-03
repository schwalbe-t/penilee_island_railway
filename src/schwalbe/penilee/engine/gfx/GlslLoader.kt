
package schwalbe.penilee.engine.gfx

import schwalbe.penilee.engine.Resource
import java.io.File
import java.nio.file.Paths

class GlslLoader(
    val vertexPath: String, val fragmentPath: String
): Resource<Shader>() {
    override fun load(): Shader { 
        val vertexSrc: String = File(vertexPath).readText()
        val fragmentSrc: String = File(fragmentPath).readText()
        return Shader(
            preprocessShader(vertexSrc, vertexPath), 
            preprocessShader(fragmentSrc, fragmentPath),
            vertexPath, fragmentPath
        )
    }
}

private val SHADER_INCLUDE_BEGIN: String = "#include \""

private fun expandShaderIncludes(src: String, path: String): String {
    val dir: String = File(path).getParentFile().getAbsolutePath()
    val expLines: List<String> = src.lines().map { line ->
        if(!line.startsWith(SHADER_INCLUDE_BEGIN)) { return@map line }
        val inclPathStartI: Int = SHADER_INCLUDE_BEGIN.length
        val inclPathEndI: Int = line.indexOf("\"", inclPathStartI)
        val includePath: String = line.slice(inclPathStartI..<inclPathEndI)
        val absIncludePath: String = Paths.get(dir, includePath).toString()
        val inclFile = File(absIncludePath)
        if(inclFile.isFile()) {
            val inclContent: String = inclFile.readText()
            return@map expandShaderIncludes(inclContent, absIncludePath)
        }
        throw RuntimeException(
            "Unable to locate included file '${includePath}' in '${path}'"
        )
    }
    return expLines.joinToString("\n")
}

private fun preprocessShader(src: String, path: String): String
    = "#version 330 core\n" + expandShaderIncludes(src, path)