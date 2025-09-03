
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import org.joml.*
import kotlin.math.*

val DEFAULT_FOV: Double = (PI * 2.0 / 360.0) * 60.0

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    loadResources(
        TEST_SHADER,
        TEST_MODEL
    )
    val state = GameState()
    window.show()
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(window, state::update, state::render)
    }
    if(!vrContext) {
        window.runLoop(DEFAULT_FOV.toFloat(), state::update, state::render)
    }
}

val TEST_SHADER = GlslLoader(
    "res/shaders/test_vert.glsl", "res/shaders/test_frag.glsl"
)
val TEST_MODEL = ObjLoader(
    "res/maxwell.obj",
    listOf(ObjAttrib.POSITION, ObjAttrib.TEX_COORDS)
)

class GameState {

    val camera = Camera()
    var modelRot: Float = 0f

    init {
        this.camera.pos.x = 1f
        this.camera.pos.y = 0.5f
        this.camera.pos.z = 1f
        this.camera.dir.x = -1f
        this.camera.dir.y = -0.5f
        this.camera.dir.z = -1f
    }

    fun update(deltaTime: Float) {
        this.modelRot += (PI * 2.0).toFloat() / 10f * deltaTime
    }

    fun render(screen: Camera, dest: Framebuffer, deltaTime: Float) {
        screen.parent = this.camera
        TEST_SHADER.get().setMatrix4("uViewProj", screen.computeViewProj())
        TEST_SHADER.get().setMatrix4("uModelTransf", Matrix4f()
            .scale(0.015f)
            .rotateY(this.modelRot)
        )
        
        dest.clearColor(Vector4f(0.2f, 0.2f, 0.2f, 1f))
        dest.clearDepth(1f)
        
        TEST_MODEL.get().render(
            TEST_SHADER.get(), dest, 1,
            "uLocalTransf", "uTexture"
        )
    }

}