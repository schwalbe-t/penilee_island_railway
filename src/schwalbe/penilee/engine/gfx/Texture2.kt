
package schwalbe.penilee.engine.gfx

import schwalbe.penilee.engine.Resource
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer

class Texture2: Texture {

    var id: Int = 0
        private set
    override val width: Int
    override val height: Int
    val owning: Boolean

    constructor(texId: Int, width: Int, height: Int, owning: Boolean) {
        this.id = texId
        this.width = width
        this.height = height
        this.owning = owning
    }

    constructor(width: Int, height: Int, data: ByteBuffer? = null) {
        this.id = glGenTextures()
        this.width = width
        this.height = height
        this.owning = true
        glBindTexture(GL_TEXTURE_2D, this.id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, data
        )
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun destroy() {
        if(!this.owning) { return }
        if(this.id != 0) { glDeleteTextures(this.id) }
        this.id = 0
    }


    class ImageLoader(val path: String): Resource<Texture2>() {
        override fun load(): Texture2 { 
            MemoryStack.stackPush().use { stack ->
                val widthPtr = stack.mallocInt(1)
                val heightPtr = stack.mallocInt(1)
                val channelsPtr = stack.mallocInt(1)
                stbi_set_flip_vertically_on_load(true)
                val imageBuffer: ByteBuffer? = stbi_load(
                    path, widthPtr, heightPtr, channelsPtr, 4
                )
                check(imageBuffer != null) { "Failed to load image ${path}" }
                val texture = Texture2(
                    widthPtr.get(0), heightPtr.get(0), imageBuffer
                )
                stbi_image_free(imageBuffer)
                return texture
            } 
        }
    }

}