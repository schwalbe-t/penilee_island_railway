
package schwalbe.penilee.engine

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.*
import schwalbe.penilee.engine.gfx.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.joml.*

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
    private var cursorHidden: Boolean = false
    val originalWidth: Int
    val originalHeight: Int
    var isFullscreen: Boolean = false
        private set

    constructor(width: Int, height: Int, name: String) {
        check(glfwInit()) { "Failed to initialize GLFW" }
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE)
        this.id = glfwCreateWindow(width, height, name, NULL, NULL)
            ?: throw RuntimeException("Failed to create window")
        this.originalWidth = width
        this.originalHeight = height
        glfwMakeContextCurrent(this.id)
        this.captureInput()
        GL.createCapabilities()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    fun captureInput() {
        glfwSetKeyCallback(this.id) { w, glfwKey, scanCode, action, m ->
            val key: Key? = GLFW_KEY_MAP[glfwKey]
            if(key == null) { return@glfwSetKeyCallback }
            when(action) {
                GLFW_PRESS -> key.press()
                GLFW_RELEASE -> key.release()
            }
        }
        glfwSetMouseButtonCallback(this.id) { w, glfwButton, action, m ->
            val button: Mouse? = GLFW_MOUSE_BUTTON_MAP[glfwButton]
            if(button == null) { return@glfwSetMouseButtonCallback }
            when(action) {
                GLFW_PRESS -> button.press()
                GLFW_RELEASE -> button.release()
            }
        }
        glfwSetCursorPosCallback(this.id) { w, x, y ->
            Mouse.moveTo(Vector2f(x.toFloat(), y.toFloat()))
        }
    }

    fun show() {
        glfwShowWindow(this.id)
        glfwFocusWindow(this.id)
    }
    fun hide() {
        glfwHideWindow(this.id)
    }

    fun makeFullscreen(fullscreen: Boolean = true) {
        val monitorId: Long = glfwGetPrimaryMonitor()
        check(monitorId != 0L) { "Failed to get primary monitor" }
        val mode: GLFWVidMode = glfwGetVideoMode(monitorId)
            ?: throw RuntimeException("Failed to get video mode")
        if(fullscreen) {
            glfwSetWindowMonitor(
                this.id, monitorId, 0, 0, mode.width(), mode.height(), 0
            )
        } else {
            val x = (mode.width() - this.originalWidth) / 2
            val y = (mode.height() - this.originalHeight) / 2
            glfwSetWindowMonitor(
                this.id, 0, x, y, this.originalWidth, this.originalHeight, 0
            )
        }
        this.isFullscreen = fullscreen
    }

    fun enableVSync(vsyncEnabled: Boolean = true) {
        glfwSwapInterval(if(vsyncEnabled) 1 else 0)
    }

    fun shouldClose(): Boolean = glfwWindowShouldClose(this.id)
    fun pollEvents() {
        Key.startFrame()
        Mouse.startFrame()
        if(Mouse.cursorHidden && !this.cursorHidden) {
            glfwSetInputMode(this.id, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        } else if(!Mouse.cursorHidden && this.cursorHidden) {
            glfwSetInputMode(this.id, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
        }
        this.cursorHidden = Mouse.cursorHidden
        glfwPollEvents()
    }
    fun swapBuffers() = glfwSwapBuffers(this.id)

    fun framebuffer(): Framebuffer = Framebuffer(0, this, this)

    fun runLoop(
        horizontalFov: Float,
        update: (Float, Camera) -> Unit, 
        render: (Camera, Framebuffer, Float) -> Unit
    ) {
        val dest = this.framebuffer()
        val camera = Camera()
        val deltaTimeState = DeltaTimeState()
        while(!this.shouldClose()) {
            this.pollEvents()
            val deltaTime: Float = deltaTimeState.computeDeltaTime()
            update(deltaTime, camera)
            camera.pos.set(ORIGIN)
            camera.dir.set(INTO_SCREEN)
            camera.up.set(UP)
            camera.setHorizontalFov(
                horizontalFov, this.width.toFloat() / this.height.toFloat()
            )
            render(camera, dest, deltaTime)
            this.swapBuffers()
        }
        dest.destroy()
    }

    override fun destroy() {}

}