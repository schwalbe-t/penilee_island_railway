
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Level {

    val player = Player(Vector3f(-2.5f, 1.5f, 1.5f))

    init {
        player.camera.dir.set(1f, 0f, -0.5f)
    }

    fun update(deltaTime: Float) {

    }

    val signalBoxes = listOf(
        Matrix4f(),
        Matrix4f()
            .translate(0f, 0f, -10f)
            .rotateY(180.degrees),
        Matrix4f()
            .translate(20f, 0f, -20f)
            .rotateY(90.degrees)
    )

    fun renderShadows(renderer: Renderer, deltaTime: Float) {
        renderer.renderShadows(SIGNAL_BOX.get(), this.signalBoxes)
    }

    fun render(renderer: Renderer, screenCam: Camera, deltaTime: Float) {
        screenCam.parent = player.camera
        renderer.applyCamera(screenCam)

        renderer.render(SIGNAL_BOX.get(), this.signalBoxes)
    }

}

fun createTestLevel(renderer: Renderer): Level {
    val sunDirection = Vector3f(-1f, -1f, 1f)
    renderer.configureLighting(
        lights = listOf(Light.sunFromAlong(
            from = Vector3f(50f, 50f, -50f),
            direction = sunDirection,
            radius = 25f,
            distance = 200f
        )),
        shadowMapRes = 4096,
        depthBias = 0.00025f,
        normalOffset = 0.055f,
        outOfBoundsLit = true,
        sunDirection = sunDirection
    )
    return Level()
}