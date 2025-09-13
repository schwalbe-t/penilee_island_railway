
package schwalbe.penilee.level

import schwalbe.penilee.*
import schwalbe.penilee.network.*
import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import kotlinx.serialization.Serializable
import org.joml.*

class SignalBox(
    val type: Type,
    val pos: Vector3fc,
    val rotationY: Float,
    val levers: List<Lever>
) {

    companion object {
        fun createLevers(
            type: SignalBox.Type, handlers: List<Layout.LeverHandler>
        ): List<Lever> {
            val startX: Float = -(handlers.size.toFloat() * Lever.WIDTH) / 2
            val posZ: Float = -(type.depth / 2f) + (Lever.DEPTH / 2f) + 0.3f
            return handlers.mapIndexed { i, h -> 
                val basePos = Vector3f(
                    startX + i.toFloat() * Lever.WIDTH, 0f, posZ
                )
                val color: Lever.Color = when(h.type) {
                    Layout.LeverHandler.Type.SIGNAL -> Lever.Color.RED
                    Layout.LeverHandler.Type.POINT -> Lever.Color.BLUE
                    Layout.LeverHandler.Type.CROSSING -> Lever.Color.BROWN
                }
                val sign: Lever.Sign = Lever.Sign.NUM_1
                Lever(basePos, color, sign, h.isLocked, h.onChange)
            }
        }
    }

    @Serializable
    class Serialized(
        val type: String,
        val pos: List<Float>,
        val dir: String
    ) {
        fun toSignalBox(levers: List<Layout.LeverHandler>): SignalBox {
            val t: SignalBox.Type = when(this.type) {
                "small" -> SignalBox.Type.SMALL
                else -> throw IllegalArgumentException(
                    "'${this.type}' is not a known signal box type"
                )
            }
            val pos = Vector3f(
                pos[0], pos[1], pos[2]
            )
            val rotationY: Float = when(this.dir) {
                "north" -> 0.degrees
                "west" -> 90.degrees
                "south" -> 180.degrees
                "east" -> 270.degrees
                else -> throw IllegalArgumentException(
                    "'${this.dir}' is not a valid signal box rotation"
                )
            }
            return SignalBox(
                t, pos, rotationY, SignalBox.createLevers(t, levers)
            )
        }
    }

    enum class Type(
        val width: Float, val depth: Float,
        val model: ObjLoader,
        val bounds: List<AABB>
    ) {
        SMALL(
            6f, 4f,
            SIGNAL_BOX, 
            listOf(
                AABB(Vector3f(-2.75f, 0f, -1.75f), Vector3f(2.75f, 0f, 1.75f))
            )
        );
    }

    fun computeBounds(): List<AABB> = this.type.bounds.map { b -> 
        AABB(
            this.transform.transformPosition(Vector3f(b.start)),
            this.transform.transformPosition(Vector3f(b.end))
        )
    }

    fun collectInteractions(): List<Interaction> {
        val leverInts: List<Interaction> = this.levers.map { it.interaction }
        return leverInts
    }

    val transform: Matrix4fc = Matrix4f()
        .translate(this.pos)
        .rotateY(this.rotationY)

    init {
        this.levers.forEach {
            it.rotationY += this.rotationY
            this.transform.transformPosition(it.basePosition)
        }   
    }
    
    private val instances: List<Matrix4fc> = listOf(this.transform)

    fun update(deltaTime: Float, player: Player, windowCam: Camera) {
        this.levers.forEach { it.update(deltaTime, player, windowCam) }
    }

    fun renderShadows(renderer: Renderer) {
        renderer.renderShadows(this.type.model.get(), this.instances)
        this.levers.forEach { it.renderShadows(renderer) }
    }

    fun render(renderer: Renderer) {
        renderer.render(this.type.model.get(), this.instances)
        this.levers.forEach { it.render(renderer) }
    }

}