
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
        TEST_MODEL,
        TEST_TEXTURE
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
    listOf(ObjAttrib.POSITION, ObjAttrib.TEX_COORDS),
    FaceCulling.BACK
)
val TEST_TEXTURE = ImageLoader("res/test.jpg")

class GameState {

    val camera = Camera()
    var cameraRot: Float = 0f

    val geometry: Geometry

    init {
        TEST_SHADER.get().setMatrix4("uModelTransf", Matrix4f())

        val builder = Geometry.Builder(listOf(
            Pair(3, Geometry.Type.FLOAT),
            Pair(2, Geometry.Type.FLOAT)
        ))
        val a = builder.putVertex { v -> v
            .putFloats(-0.5f, +1.0f, 0f)
            .putFloats(0f, 1f)
        }
        val b = builder.putVertex { v -> v
            .putFloats(+0.5f, +1.0f, 0f)
            .putFloats(1f, 1f)
        }
        val c = builder.putVertex { v -> v
            .putFloats(-0.5f,  0.0f, 0f)
            .putFloats(0f, 0f)
        }
        val d = builder.putVertex { v -> v
            .putFloats(+0.5f,  0.0f, 0f)
            .putFloats(1f, 0f)
        }
        builder.putElement(c, b, a)
        builder.putElement(b, c, d)
        geometry = builder.build()  
    }

    fun update(deltaTime: Float) {
        this.cameraRot += (PI * 2.0).toFloat() / 10f * deltaTime
        // this.camera.pos.x = cos(this.cameraRot) * 3f
        this.camera.pos.x = 0f
        this.camera.pos.z = 2f
    }

    fun render(screen: Camera, dest: Framebuffer, deltaTime: Float) {
        screen.parent = this.camera
        TEST_SHADER.get().setMatrix4("uViewProj", screen.computeViewProj())
        
        dest.clearColor(Vector4f(0.2f, 0.2f, 0.2f, 1f))
        dest.clearDepth(1f)
        
        // TEST_MODEL.get().render(
        //     TEST_SHADER.get(), dest, 1,
        //     "uLocalTransf", "uTexture"    
        // )

        TEST_SHADER.get()
            .setMatrix4("uLocalTransf", Matrix4f())
            .setTexture2("uTexture", TEST_TEXTURE.get())
        this.geometry.render(TEST_SHADER.get(), dest)
    }

}