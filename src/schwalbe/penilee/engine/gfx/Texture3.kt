
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer

class Texture3: Texture {

    var id: Int = 0
        private set
    override val width: Int
    override val height: Int
    var layers: Int
    val owning: Boolean

    constructor(
        texId: Int, width: Int, height: Int, layers: Int, owning: Boolean
    ) {
        this.id = texId
        this.width = width
        this.height = height
        this.layers = layers
        this.owning = owning
    }

    constructor(
        width: Int, height: Int, layers: Int, data: ByteBuffer? = null
    ) {
        this.id = glGenTextures()
        this.width = width
        this.height = height
        this.layers = layers
        this.owning = true
        glBindTexture(GL_TEXTURE_2D_ARRAY, this.id)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage3D(
            GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, width, height, layers, 
            0, GL_RGBA, GL_UNSIGNED_BYTE, data
        )
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
    }

    fun destroy() {
        if(!this.owning) { return }
        if(this.id != 0) { glDeleteTextures(this.id) }
        this.id = 0
    }

}