
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL32
import org.joml.*

class Framebuffer {

    val texture: Texture
    val layer: Int
    val id: Int

    constructor(texture: Texture, layer: Int = 0) {
        this.texture = texture
        this.layer = layer
        this.id = GL30.glGenFramebuffers()
        this.bind {
            GL32.glFramebufferTextureLayer(
                GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
                texture.id, 0, layer
            )
            GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0)
            val status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
            check(status == GL30.GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer is incomplete"
            }
        }
    }

    inline fun bind(f: () -> Unit) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.id)
        GL30.glViewport(0, 0, this.texture.width, this.texture.height)
        try {
            f()
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        }
    }

    fun clear(color: Vector4f) {
        this.bind {
            GL30.glClearColor(color.x(), color.y(), color.z(), color.w())
            GL30.glClear(GL30.GL_COLOR_BUFFER_BIT)
        }
    }

    fun destroy() {
        GL30.glDeleteFramebuffers(this.id)
    }

}