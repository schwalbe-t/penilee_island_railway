
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.input.*
import schwalbe.penilee.resources.*
import org.joml.*
import kotlin.math.*

class Lever(
    val basePosition: Vector3f, color: Color, sign: Sign,
    val isLocked: () -> Boolean, val onChange: (Boolean) -> Unit
) {

    enum class Color(val texture: ImageLoader) {
        RED(LEVER_BODY_RED),
        BLUE(LEVER_BODY_BLUE),
        BROWN(LEVER_BODY_BROWN);
    }

    enum class Sign(val texture: ImageLoader) {
        NUM_1(LEVER_SIGN_NUM_1),
        NUM_2(LEVER_SIGN_NUM_2),
        NUM_3(LEVER_SIGN_NUM_3)
    }
    

    companion object {
        val MAX_ANGLE: Float = 10.degrees
        val ROTATION_ANCHOR_OFFSET: Vector3fc = Vector3f(0f, -2f, 0f)
        val CLUTCH_OFFSET: Vector3fc = Vector3f(0f, 0.8225f, 0f)
        val CLUTCH_MAX_ANGLE: Float = 15.degrees
        val INTERACTION_OFFSET: Vector3fc = Vector3f(0f, 1.2f, 0f)
        val MOUSE_MAX_SELECT_DIST: Float = 0.25f // in NDC
        val MOUSE_MOVE_SPEED: Float = 1f // per second
    }


    private val bodyTextures: Map<Pair<String, String>, Texture2> = mapOf(
        Pair("lever_body", "lever_body") to color.texture.get(),
        Pair("lever_clutch", "lever_body") to color.texture.get(),
        Pair("lever_sign", "lever_sign") to sign.texture.get()
    )

    val interaction = Interaction() { c -> c.squeeze >= 0.7f }

    var rotationY: Float = 0.degrees
    var angle: Float = 0f
    var clutch: Float = 0f

    private var state: Boolean = false

    private var baseTransform = mutableListOf<Matrix4fc>(Matrix4f())
    private var bodyTransform = mutableListOf<Matrix4fc>(Matrix4f())
    private var clutchTransform = mutableListOf<Matrix4fc>(Matrix4f())

    fun updateTransforms(): Lever {
        this.baseTransform[0] = Matrix4f()
            .translate(this.basePosition)
        val relBodyTransform = Matrix4f()
            .rotateY(this.rotationY)
            .translate(Lever.ROTATION_ANCHOR_OFFSET)
            .rotateX((this.angle * 2f - 1f) * Lever.MAX_ANGLE)
            .translate(Vector3f(Lever.ROTATION_ANCHOR_OFFSET).negate())
        this.bodyTransform[0] = Matrix4f(this.baseTransform[0])
            .mul(relBodyTransform)
        val relClutchTransform = Matrix4f()
            .translate(CLUTCH_OFFSET)
            .rotateX(clutch * Lever.CLUTCH_MAX_ANGLE)
        this.clutchTransform[0] = Matrix4f(this.bodyTransform[0])
            .mul(relClutchTransform)
        return this
    }

    fun update(deltaTime: Float, player: Player, windowCam: Camera) {
        val locked: Boolean = this.isLocked()
        val gripping: Hand? = this.interaction.gripping
        if(gripping != null) {
            this.interaction.locked = gripping.controller.squeeze > 0.5f
            this.clutch = gripping.controller.trigger
            val leverToController: Vector3fc = Vector3f(gripping.position)
                .sub(this.interaction.position)
            val leverDisable: Vector3f = Vector3f(INTO_SCREEN)
                .rotateY(this.rotationY)
                .negate()
            val dot: Float = leverToController.dot(leverDisable)
            val newAngle: Float = if(clutch >= 0.5f) this.angle + dot
                else this.angle
            val reachedEnd: Boolean = (!(this.angle <= 0f) && newAngle <= 0f)
                || (!(this.angle >= 1f) && newAngle >= 1f)
            if(reachedEnd) {
                gripping.controller.vibrate(0.5f, 100_000_000)
            }
            this.angle = newAngle
            gripping.position = Vector3f(this.interaction.position)
        } else if(!locked && this.interaction.isClosest) {
            this.interaction.locked = Mouse.LEFT.isPressed
                || Mouse.RIGHT.isPressed
            this.clutch = 1f
            if(Mouse.LEFT.isPressed && this.angle > 0f) {
                this.angle -= Lever.MOUSE_MOVE_SPEED * deltaTime
            }
            if(Mouse.RIGHT.isPressed && this.angle < 1f) {
                this.angle += Lever.MOUSE_MOVE_SPEED * deltaTime
            }
        } else {
            this.clutch = 0f
        }
        this.angle = min(max(this.angle, 0f), 1f)
        val newState: Boolean = when(this.angle) {
            1f -> true
            0f -> false
            else -> state
        }
        if(newState != this.state) {
            this.state = newState
            this.onChange(newState)
        }
        this.updateTransforms()
        this.interaction.position = this.bodyTransform[0]
            .transformPosition(Vector3f(Lever.INTERACTION_OFFSET))
    }

    fun renderShadows(renderer: Renderer) {
        renderer.renderShadows(
            LEVER_BASE.get(), this.baseTransform
        )
        renderer.renderShadows(
            LEVER_BODY.get(), this.bodyTransform,
            texOverrides = this.bodyTextures
        )
        renderer.renderShadows(
            LEVER_CLUTCH.get(), this.clutchTransform,
            texOverrides = this.bodyTextures
        )
    }

    fun render(renderer: Renderer) {
        renderer.render(
            LEVER_BASE.get(), this.baseTransform
        )
        renderer.render(
            LEVER_BODY.get(), this.bodyTransform, 
            texOverrides = this.bodyTextures
        )
        renderer.render(
            LEVER_CLUTCH.get(), this.clutchTransform, 
            texOverrides = this.bodyTextures
        )
    }

}