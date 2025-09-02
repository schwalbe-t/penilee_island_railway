
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
import org.joml.*

class Framebuffer {

    val fboId: Int
    private var rboId: Int = 0
    private var rboWidth: Int = 0
    private var rboHeight: Int = 0
    var texture: Texture? = null
        private set
    val owning: Boolean

    constructor() {
        this.fboId = glGenFramebuffers()
        this.owning = true
    }

    constructor(fboId: Int, texture: Texture?) {
        this.fboId = fboId
        this.texture = texture
        this.owning = false
    }

    fun attach(texture: Texture2): Framebuffer {
        check(this.owning) { "Attempt to attach to immutable framebuffer" }
        this.bindRaw()
        glFramebufferTexture(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 
            texture.id, 0
        )
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        this.texture = texture
        return this
    }

    fun attach(texture: Texture3, layer: Int): Framebuffer {
        check(this.owning) { "Attempt to attach to immutable framebuffer" }
        this.bindRaw()
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 
            texture.id, 0, layer
        )
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        this.texture = texture
        return this
    }

    private fun initRenderBuffer(width: Int, height: Int) {
        if(!this.owning) { return }
        if(width == this.rboWidth && height == this.rboHeight) { return }
        if(this.rboId != 0) {
            glDeleteRenderbuffers(this.rboId)
        }
        this.rboId = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, this.rboId)
        glRenderbufferStorage(
            GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, 
            width, height
        )
        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, 
            GL_RENDERBUFFER, this.rboId
        )
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
        this.rboWidth = width
        this.rboHeight = height
    }

    private inline fun bindRaw() = glBindFramebuffer(GL_FRAMEBUFFER, this.fboId)

    fun bind() {
        this.bindRaw()
        val tex = this.texture
        check(tex != null) { "Attempt to bind incomplete framebuffer" }
        this.initRenderBuffer(tex.width, tex.height)
        glViewport(0, 0, tex.width, tex.height)
    }

    fun clearColor(color: Vector4f): Framebuffer {
        this.bind()
        glClearColor(color.x(), color.y(), color.z(), color.w())
        glClear(GL_COLOR_BUFFER_BIT)
        return this
    }

    fun clearDepth(value: Float): Framebuffer {
        this.bind()
        glClearDepth(value.toDouble())
        glClear(GL_DEPTH_BUFFER_BIT)
        return this
    }

    fun destroy() {
        if(!this.owning) { return }
        glDeleteFramebuffers(this.fboId)
        if(this.rboId != 0) {
            glDeleteRenderbuffers(this.rboId)
        }
    }

}