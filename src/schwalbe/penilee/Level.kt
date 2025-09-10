
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Level {

    val player = Player(
        Vector3f(0f, 0f, 1.5f),
        listOf(
            AABB(Vector3f(-2.75f, 0f, -1.75f), Vector3f(2.75f, 0f, 1.75f))
        )
    )
    val camera = Camera()
    val hands = listOf(
        Hand(VrController.LEFT, Vector3f(-1f, 1f, 1f)), 
        Hand(VrController.RIGHT, Vector3f(1f, 1f, 1f))
    )

    init {
        this.hands.forEach { it.hands = this.hands }
    }

    val levers: List<Lever> = listOf(
        Lever(
            Vector3f(-2.00f, 0f, -1.20f), Lever.Color.RED, Lever.Sign.NUM_3,
            { true }, { state -> }
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
        
        this.hands.forEach { it.inheritPosition(this.player) }
        interactions.update(this.player, windowCam, this.hands, inVr)
        this.levers.forEach { it.update(deltaTime, this.player, windowCam) }
        this.hands.forEach { it.update(inVr, deltaTime) }

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
        this.hands.forEach { it.renderShadows(renderer) }
    }

    fun render(renderer: Renderer, screenCam: Camera, deltaTime: Float) {
        screenCam.parent = this.camera
        renderer.applyCamera(screenCam)

        renderer.render(SIGNAL_BOX.get(), listOf(Matrix4f()))
        this.levers.forEach { it.render(renderer) }
        this.hands.forEach { it.render(renderer) }
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