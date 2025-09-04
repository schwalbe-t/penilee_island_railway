
package schwalbe.penilee.engine

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

class Window: Texture {

    private val id: Long
    override val width: Int
        get() = MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            glfwGetFramebufferSize(this.id, width, null)
            return width.get(0)
        }
    override val height: Int
        get() = MemoryStack.stackPush().use { stack ->
            val height = stack.mallocInt(1)
            glfwGetFramebufferSize(this.id, null, height)
            return height.get(0)
        }

    constructor(width: Int, height: Int, name: String) {
        check(glfwInit()) { "Failed to initialize GLFW" }
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        this.id = glfwCreateWindow(width, height, name, NULL, NULL)
            ?: throw RuntimeException("Failed to create window")
        glfwMakeContextCurrent(this.id)
        GL.createCapabilities()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    fun show() = glfwShowWindow(this.id)
    fun hide() = glfwHideWindow(this.id)

    fun shouldClose(): Boolean = glfwWindowShouldClose(this.id)
    fun pollEvents() = glfwPollEvents()
    fun swapBuffers() = glfwSwapBuffers(this.id)

    fun framebuffer(): Framebuffer = Framebuffer(0, this)

    fun runLoop(
        horizontalFov: Float,
        update: (Float) -> Unit, 
        render: (Camera, Framebuffer, Float) -> Unit
    ) {
        val dest = this.framebuffer()
        val camera = Camera()
        val deltaTimeState = DeltaTimeState()
        while(!this.shouldClose()) {
            this.pollEvents()
            val deltaTime: Float = deltaTimeState.computeDeltaTime()
            update(deltaTime)
            camera.pos.set(ORIGIN)
            camera.dir.set(NDC_INTO_SCREEN)
            camera.up.set(UP)
            camera.setHorizontalFov(
                horizontalFov, this.width.toFloat() / this.height.toFloat()
            )
            render(camera, dest, deltaTime)
            this.swapBuffers()
        }
        dest.destroy()
    }

}