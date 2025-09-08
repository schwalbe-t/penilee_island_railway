
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Level {

    val player = Player(
        Vector3f(),
        listOf(
            AABB(Vector3f(-2.75f, 0f, -1.75f), Vector3f(2.75f, 0f, 1.75f))
        )
    )
    val camera = Camera()

    fun update(deltaTime: Float, inVr: Boolean, windowCam: Camera) {
        this.player.update(deltaTime, inVr, windowCam)
        this.player.configureCamera(this.camera)

        if(VrController.Button.STICK_L.isPressed) {
            VrController.LEFT.vibrate(1.0f, 100_000_000)
        }
        if(VrController.Button.STICK_R.isPressed) {
            VrController.RIGHT.vibrate(1.0f, 100_000_000)
        }
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
        screenCam.parent = this.camera
        renderer.applyCamera(screenCam)

        renderer.render(SIGNAL_BOX.get(), this.signalBoxes)

        renderer.render(SIGNAL_BOX.get(), listOf(
            Matrix4f()
                .translate(this.camera.pos)
                .translate(Vector3f(VrController.RIGHT.gripPos)
                    .rotateY(this.player.angleY)
                )
                .scale(0.01f)
                .rotateY(this.player.angleY)
                .rotateTowards(
                    VrController.RIGHT.gripDir, VrController.RIGHT.gripUp
                )
                .rotateZ(180.degrees)
                .rotateX(90.degrees)
        ))
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