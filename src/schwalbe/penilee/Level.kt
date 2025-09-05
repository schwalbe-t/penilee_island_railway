
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Level {

    val player = Player(Vector3f(-2f, 1.5f, 1.5f))

    fun update(deltaTime: Float) {

    }

    fun render(screenCam: Camera, dest: Framebuffer, deltaTime: Float) {
        screenCam.parent = player.camera

        val shader: Shader = TEST_SHADER.get()
        shader.setMatrix4("uViewProj", screenCam.computeViewProj())
        shader.setMatrix4("uModelTransf", Matrix4f())

        dest.clearColor(Vector4f(109f, 128f, 250f, 255f).div(255f))
        dest.clearDepth(1f)

        SIGNAL_BOX.get().render(
            TEST_SHADER.get(), dest, 1,
            "uLocalTransf", "uTexture"
        )
    }

}