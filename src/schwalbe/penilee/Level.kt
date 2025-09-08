
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

    val levers: List<Lever> = listOf(
        Lever(
            Vector3f(-2.00f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-1.75f, 0f, -1.20f), Lever.Color.BLUE, Lever.Sign.NUM_2,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-1.50f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-1.25f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-1.00f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-0.75f, 0f, -1.20f), Lever.Color.BLUE, Lever.Sign.NUM_2,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-0.50f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(-0.25f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f( 0.00f, 0f, -1.20f), Lever.Color.BLUE, Lever.Sign.NUM_2,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+0.25f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+0.50f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+0.75f, 0f, -1.20f), Lever.Color.BLUE, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+1.00f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_2,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+1.25f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+1.50f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_3,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+1.75f, 0f, -1.20f), Lever.Color.BLUE, Lever.Sign.NUM_1,
            { false }, { state -> }
        ),
        Lever(
            Vector3f(+2.00f, 0f, -1.20f), Lever.Color.BROWN, Lever.Sign.NUM_2,
            { false }, { state -> }
        )
    )

    val interactions = Interaction.Manager(levers.map { it.interaction })

    fun update(deltaTime: Float, inVr: Boolean, windowCam: Camera) {
        this.player.update(deltaTime, inVr, windowCam)
        this.player.configureCamera(this.camera)

        interactions.update(this.player, windowCam, inVr)

        this.levers.forEach { it.update(deltaTime, this.player, windowCam) }

        if(VrController.Button.STICK_L.isPressed) {
            VrController.LEFT.vibrate(1.0f, 100_000_000)
        }
        if(VrController.Button.STICK_R.isPressed) {
            VrController.RIGHT.vibrate(1.0f, 100_000_000)
        }
    }

    fun renderShadows(renderer: Renderer, deltaTime: Float) {
        renderer.renderShadows(SIGNAL_BOX.get(), listOf(Matrix4f()))
        this.levers.forEach { it.renderShadows(renderer) }
    }

    fun render(renderer: Renderer, screenCam: Camera, deltaTime: Float) {
        screenCam.parent = this.camera
        renderer.applyCamera(screenCam)

        renderer.render(SIGNAL_BOX.get(), listOf(Matrix4f()))
        this.levers.forEach { it.render(renderer) }

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