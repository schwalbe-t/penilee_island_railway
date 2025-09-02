
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import org.joml.*
import kotlin.math.*

val DEFAULT_FOV: Double = (PI * 2.0 / 360.0) * 60.0

fun main() {
    val window = Window(1280, 720, "Penilee Island Railway")
    loadResources(
        TEST_TEXTURE
    )
    val state = GameState()
    val vrContext: Boolean = withVrContext { vr ->
        vr.runLoop(state::update, state::render)
    }
    if(!vrContext) {
        window.show()
        window.runLoop(DEFAULT_FOV.toFloat(), state::update, state::render)
    }
}

val TEST_TEXTURE = Texture2.ImageLoader("test.jpg")

class GameState {

    val shader = Shader(
        """
            #version 330 core

            layout(location = 0) in vec2 vPos;
            layout(location = 1) in vec2 vUv;
            
            out vec2 fUv;

            void main() {
                gl_Position = vec4(vPos, 0.0, 1.0);
                fUv = vUv;
            }
        """.trimIndent(),
        """
            #version 330 core

            in vec2 fUv;

            uniform sampler2D uTexture;

            out vec4 oColor;

            void main() {
                oColor = texture(uTexture, fUv);
            }
        """.trimIndent()
    )

    val mesh: Mesh

    init {
        this.shader.setTexture2("uTexture", TEST_TEXTURE.get())

        val builder = Mesh.Builder(arrayOf(
            Pair(2, Mesh.Type.FLOAT),
            Pair(2, Mesh.Type.FLOAT)
        ))
        val a = builder.putVertex { it
            .putFloats( 0.0f, +0.5f)
            .putFloats(+0.5f, +1.0f)
        }
        val b = builder.putVertex { it
            .putFloats(-0.5f, -0.5f)
            .putFloats( 0.0f,  0.0f)
        }
        val c = builder.putVertex { it
            .putFloats(+0.5f, -0.5f)
            .putFloats(+1.0f,  0.0f)
        }
        builder.putElement(a, b, c)
        this.mesh = builder.build()
    }

    fun update() {}

    fun render(screen: Camera, dest: Framebuffer) {
        dest.clearColor(Vector4f(abs(screen.dir.x()), abs(screen.dir.y()), abs(screen.dir.z()), 1f))
        dest.clearDepth(1f)
        this.mesh.render(this.shader, dest)
    }

}