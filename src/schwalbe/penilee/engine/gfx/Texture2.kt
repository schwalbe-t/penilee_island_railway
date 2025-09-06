
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
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

    constructor(
        width: Int, height: Int, fmt: TextureFormat, data: ByteBuffer? = null
    ) {
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
            GL_TEXTURE_2D, 0, fmt.iFmt, width, height, 0,
            fmt.fmt, fmt.chType, data
        )
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    override fun destroy() {
        if(!this.owning) { return }
        if(this.id != 0) { glDeleteTextures(this.id) }
        this.id = 0
    }

}