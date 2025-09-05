
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
import org.joml.*

class Framebuffer {

    var fboId: Int = 0
        private set
    val owning: Boolean
    var color: Texture? = null
        private set
    var depth: Texture? = null
        private set

    constructor() {
        this.fboId = glGenFramebuffers()
        this.owning = true
        this.bindRaw()
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
    }

    constructor(fboId: Int, color: Texture?, depth: Texture?) {
        this.fboId = fboId
        this.color = color
        this.depth = depth
        this.owning = false
    }

    private fun checkValidAttachment(tex: Texture) {
        check(this.owning) { "Attempt to attach to immutable framebuffer" }
        val existing: Texture? = this.color ?: this.depth
        if(existing == null) { return }
        check(tex.width == existing.width && tex.height == existing.height) {
            "Attachment size does not match previously attached textures"
        }
    }

    fun attachColor(texture: Texture2): Framebuffer {
        this.checkValidAttachment(texture)
        this.bindRaw()
        glFramebufferTexture(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texture.id, 0
        )
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        this.color = texture
        return this
    }

    fun attachColor(texture: Texture3, layer: Int): Framebuffer {
        this.checkValidAttachment(texture)
        this.bindRaw()
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 
            texture.id, 0, layer
        )
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        this.color = texture
        return this
    }

    fun detachColor(): Framebuffer {
        check(this.owning) { "Attempt to detach from immutable framebuffer" }
        this.bindRaw()
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 0, 0)
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        this.color = null
        return this
    }

    fun attachDepth(texture: Texture2): Framebuffer {
        this.checkValidAttachment(texture)
        this.bindRaw()
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, texture.id, 0)
        this.depth = texture
        return this
    }

    fun attachDepth(texture: Texture3, layer: Int): Framebuffer {
        this.checkValidAttachment(texture)
        this.bindRaw()
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, 
            texture.id, 0, layer
        )
        this.depth = texture
        return this
    }
    
    fun detachDepth(): Framebuffer {
        check(this.owning) { "Attempt to detach from immutable framebuffer" }
        this.bindRaw()
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, 0, 0)
        this.depth = null
        return this
    }

    private fun bindRaw() = glBindFramebuffer(GL_FRAMEBUFFER, this.fboId)

    fun bind() {
        this.bindRaw()
        val target: Texture? = this.color ?: this.depth
        if(target != null) {
            glViewport(0, 0, target.width, target.height)
        }
    }

    fun clearColor(color: Vector4f): Framebuffer {
        if(this.color == null) { return this }
        this.bind()
        glClearColor(color.x(), color.y(), color.z(), color.w())
        glClear(GL_COLOR_BUFFER_BIT)
        return this
    }

    fun clearDepth(value: Float): Framebuffer {
        if(this.depth == null) { return this }
        this.bind()
        glClearDepth(value.toDouble())
        glClear(GL_DEPTH_BUFFER_BIT)
        return this
    }

    fun destroy() {
        if(!this.owning) { return }
        if(this.fboId != 0) { glDeleteFramebuffers(this.fboId) }
        this.fboId = 0
    }

}