
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.joml.*

class Shader {

    var id: Int = 0
        private set

    class TextureEntry(val uniform: String, var id: Int, val glType: Int)

    private val textures = mutableListOf<TextureEntry>()

    constructor(
        vertexSrc: String, fragmentSrc: String,
        vertexPath: String = "<unknown>", fragmentPath: String = "<unknown>"
    ) {
        val vertId = this.compileShader(
            GL_VERTEX_SHADER, "Vertex", vertexSrc, vertexPath
        )
        val fragId = this.compileShader(
            GL_FRAGMENT_SHADER, "Fragment", fragmentSrc, fragmentPath
        )
        this.id = glCreateProgram()
        glAttachShader(this.id, vertId)
        glAttachShader(this.id, fragId)
        glLinkProgram(this.id)
        if(glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
            val infoLog = glGetProgramInfoLog(this.id)
            glDeleteProgram(this.id)
            throw RuntimeException(
                "Shaders '${vertexPath}' and '${fragmentPath}' failed to link"
                    + "\n\n--- Vertex Shader Source ---\n${vertexSrc}"
                    + "\n\n--- Fragment Shader Source ---\n${fragmentSrc}"
                    + "\n\n--- Info Log ---\n${infoLog}"
            )
        }
        glDetachShader(this.id, vertId)
        glDetachShader(this.id, fragId)
        glDeleteShader(vertId)
        glDeleteShader(fragId)
    }

    private fun compileShader(
        glType: Int, dispType: String, source: String, path: String
    ): Int {
        val id: Int = glCreateShader(glType)
        glShaderSource(id, source)
        glCompileShader(id)
        if(glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            val infoLog = glGetShaderInfoLog(id)
            glDeleteShader(id)
            throw RuntimeException(
                "${dispType} shader '${path}' failed to compile"
                    + "\n\n--- ${dispType} Shader Source ---\n${source}"
                    + "\n\n--- Info Log ---\n${infoLog}"
            )
        }
        return id
    }

    inline private fun findUniform(name: String, f: (Int) -> Unit): Shader {
        this.bindRaw()
        val loc = glGetUniformLocation(this.id, name)
        // We can't hard error here because the variable might've been
        // optimized away
        if(loc != -1) { f(loc) }
        return this
    }

    private fun allocateTextureSlot(n: String, id: Int, glType: Int): Int {
        for(slot in 0..<this.textures.size) {
            val entry = this.textures[slot]
            if(entry.uniform != n) { continue }
            entry.id = id
            assert(entry.glType == glType)
            return slot
        }
        val slot: Int = this.textures.size
        this.textures.add(TextureEntry(n, id, glType))
        return slot
    }

    fun setTexture2(n: String, v: Texture2): Shader
        = this.findUniform(n) { l ->
        val slot = this.allocateTextureSlot(n, v.id, GL_TEXTURE_2D)
        glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D, v.id)
        glUniform1i(l, slot)
    }
    fun setTexture3(n: String, v: Texture3): Shader
        = this.findUniform(n) { l ->
        val slot = this.allocateTextureSlot(n, v.id, GL_TEXTURE_2D_ARRAY)
        glActiveTexture(GL_TEXTURE0 + slot)
        glBindTexture(GL_TEXTURE_2D_ARRAY, v.id)
        glUniform1i(l, slot)
    }
    
    fun setFloat(n: String, v: Float): Shader
        = this.findUniform(n) { l -> glUniform1f(l, v) }
    fun setVec2(n: String, v: Vector2fc): Shader
        = this.findUniform(n) { l -> glUniform2f(l, v.x(), v.y()) }
    fun setVec3(n: String, v: Vector3fc): Shader
        = this.findUniform(n) { l -> glUniform3f(l, v.x(), v.y(), v.z()) }
    fun setVec4(n: String, v: Vector4fc): Shader 
        = this.findUniform(n) { l -> 
        glUniform4f(l, v.x(), v.y(), v.z(), v.w()) 
    }
    fun setMatrix2(n: String, v: Matrix2fc): Shader 
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(2 * 2)
            v.get(0, buff)
            glUniformMatrix2fv(l, false, buff)
        }
    }
    fun setMatrix3(n: String, v: Matrix3fc): Shader 
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(3 * 3)
            v.get(0, buff)
            glUniformMatrix3fv(l, false, buff)
        }
    }
    fun setMatrix4(n: String, v: Matrix4fc): Shader 
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(4 * 4)
            v.get(0, buff)
            glUniformMatrix4fv(l, false, buff)
        }
    }
    fun setFloatArr(n: String, v: List<Float>): Shader 
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(v.size)
            v.forEachIndexed { index, value -> buff.put(index, value) }
            glUniform1fv(l, buff)
        }
    }
    fun setVec2Arr(n: String, v: List<Vector2fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(2 * v.size)
            v.forEachIndexed { index, value -> value.get(2 * index, buff) }
            glUniform2fv(l, buff)
        }
    }
    fun setVec3Arr(n: String, v: List<Vector3fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(3 * v.size)
            v.forEachIndexed { index, value -> value.get(3 * index, buff) }
            glUniform3fv(l, buff)
        }
    }
    fun setVec4Arr(n: String, v: List<Vector4fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(4 * v.size)
            v.forEachIndexed { index, value -> value.get(4 * index, buff) }
            glUniform4fv(l, buff)
        }
    }
    fun setMatrix2Arr(n: String, v: List<Matrix2fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(2 * 2 * v.size)
            v.forEachIndexed { index, value -> value.get(2 * 2 * index, buff) }
            glUniformMatrix2fv(l, false, buff)
        }
    }
    fun setMatrix3Arr(n: String, v: List<Matrix3fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(3 * 3 * v.size)
            v.forEachIndexed { index, value -> value.get(3 * 3 * index, buff) }
            glUniformMatrix3fv(l, false, buff)
        }
    }
    fun setMatrix4Arr(n: String, v: List<Matrix4fc>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocFloat(4 * 4 * v.size)
            v.forEachIndexed { index, value -> value.get(4 * 4 * index, buff) }
            glUniformMatrix4fv(l, false, buff)
        }
    }

    fun setInt(n: String, v: Int): Shader
        = this.findUniform(n) { l -> glUniform1i(l, v) }
    fun setIntArr(n: String, v: List<Int>): Shader
        = this.findUniform(n) { l -> 
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocInt(v.size)
            v.forEachIndexed { index, value -> buff.put(index, value) }
            glUniform1iv(l, buff)
        } 
    }

    fun setUInt(n: String, v: UInt): Shader
        = this.findUniform(n) { l -> glUniform1ui(l, v.toInt()) }
    fun setUIntArr(n: String, v: List<UInt>): Shader
        = this.findUniform(n) { l ->
        MemoryStack.stackPush().use { stack ->
            val buff = stack.mallocInt(v.size)
            v.forEachIndexed { index, value -> buff.put(index, value.toInt()) }
            glUniform1uiv(l, buff)
        }
    }

    private inline fun bindRaw() = glUseProgram(this.id)

    fun bind() {
        this.bindRaw()
        for(slot in 0..<this.textures.size) {
            val entry = this.textures[slot]
            glActiveTexture(GL_TEXTURE0 + slot)
            glBindTexture(entry.glType, entry.id)
        }
    }

    fun destroy() {
        if(this.id != 0) { glDeleteProgram(this.id) }
        this.id = 0
    }

}