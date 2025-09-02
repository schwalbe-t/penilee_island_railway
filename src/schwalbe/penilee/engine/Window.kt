
package schwalbe.penilee.engine

import schwalbe.penilee.engine.gfx.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
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
    }

    fun show() = glfwShowWindow(this.id)
    fun hide() = glfwHideWindow(this.id)

    fun runLoop(
        horizontalFov: Float,
        update: () -> Unit, 
        render: (Camera, Framebuffer) -> Unit
    ) {
        val dest = Framebuffer(0, this)
        val screen = Camera()
        while(!glfwWindowShouldClose(this.id)) {
            glfwPollEvents()
            screen.setHorizontalFov(
                horizontalFov, this.width.toFloat() / this.height.toFloat()
            )
            update()
            render(screen, dest)
            glfwSwapBuffers(this.id)
        }
        dest.destroy()
    }

}