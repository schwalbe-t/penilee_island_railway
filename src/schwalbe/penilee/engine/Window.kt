
package schwalbe.penilee.engine

import schwalbe.penilee.engine.gfx.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

class Window {

    val id: Long

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
        val destTex = Texture(this.width(), this.height())
        val dest = Framebuffer(destTex)
        val screen = Camera()
        while(!glfwWindowShouldClose(this.id)) {
            glfwPollEvents()
            screen.setHorizontalFov(
                horizontalFov, this.width().toFloat() / this.height().toFloat()
            )
            destTex.resizeFast(this.width(), this.height())
            update()
            render(screen, dest)
            this.displayTexture(destTex)
            glfwSwapBuffers(this.id)
        }
        dest.destroy()
        destTex.destroy()
    }

    fun width(): Int {
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            glfwGetFramebufferSize(this.id, width, null)
            return width.get(0)
        }
    }
    fun height(): Int {
        MemoryStack.stackPush().use { stack ->
            val height = stack.mallocInt(1)
            glfwGetFramebufferSize(this.id, null, height)
            return height.get(0)
        }
    }

    fun displayTexture(tex: Texture) {
        // TODO! not yet implemented
    }

}