
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.VrController
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Hand(
    val controller: VrController, val scale: Vector3fc
) {

    var hands: List<Hand> = listOf()

    class ClothAttachment(val x: Int, val y: Int, val offset: Vector3fc)

    companion object {
        val CLOTH_ORIGIN_OFFSET: Vector3fc = Vector3f(0.01f, 0f, -0.1f)
        val CLOTH_ATTACHMENTS: List<ClothAttachment> = listOf(
            ClothAttachment(0, 0, Vector3f(-0.01f, -0.050f, +0.05f)),
            ClothAttachment(4, 0, Vector3f(-0.01f, -0.025f, +0.05f)),
            ClothAttachment(8, 0, Vector3f(-0.01f,  0.000f, +0.05f))
        )
        val CLOTH_COLLIDERS: List<Cloth.Collider> = listOf(
            Cloth.Collider(Vector3f( 0.000f, -0.12f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f( 0.000f, -0.12f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.015f, -0.09f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.015f, -0.09f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, -0.06f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, -0.06f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, -0.03f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, -0.03f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f,  0.00f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f,  0.00f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, +0.03f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, +0.03f, -0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, +0.06f, +0.02f), 0.030f),
            Cloth.Collider(Vector3f(-0.030f, +0.06f, -0.02f), 0.030f)
        )
    }


    var cloth: Cloth? = null
        private set

    var isActive: Boolean = false
        private set
    val model: ObjLoader
        get() = if(this.controller.squeeze > 0.5f) HAND_GRIP else HAND_IDLE
    var position: Vector3f = Vector3f()
    var direction: Vector3f = Vector3f()
    var up: Vector3f = Vector3f()
    var transform: Matrix4f = Matrix4f()

    fun addCloth() {
        if(this.cloth != null) { return }
        val origin: Vector3f = this.transform
            .transformPosition(Vector3f(+0.1f, 0f, +0.04f))
        val dirX: Vector3f = this.transform
            .transformDirection(Vector3f(0f, -1f, -1f).normalize())
        val dirY: Vector3f = this.transform
            .transformDirection(Vector3f(0f, -1f, +1f).normalize())
        this.cloth = Cloth(
            origin = origin, dirX = dirX, dirY = dirY,
            width = 32, height = 32, nodeDist = 1f/32f/4f, 
            texture = CLOTH_PATTERN.get(), 
            stiffness = 150000f, mass = 2f, damping = 0.02f,
            colliders = List(Hand.CLOTH_COLLIDERS.size * this.hands.size) {
                Cloth.Collider(Vector3f(), 0f) 
            }
        )
    }

    fun removeCloth() {
        this.cloth?.destroy()
        this.cloth = null
    }

    fun inheritPosition(player: Player) {
        if(!this.isActive) { return } 
        this.position = Vector3f(this.controller.gripPos)
            .rotateY(player.angleY)
            .add(player.computeHeadPos())
        this.direction = Vector3f(this.controller.gripDir)
            .rotateY(player.angleY)
        this.up = Vector3f(this.controller.gripUp)
            .rotateY(player.angleY)
    }

    private fun updateClothColliders(cloth: Cloth, handIdx: Int) {
        val startIdx: Int = handIdx * Hand.CLOTH_COLLIDERS.size
        for(collIdx in 0..<Hand.CLOTH_COLLIDERS.size) {
            val hand: Hand = this.hands[handIdx]
            val coll: Cloth.Collider = cloth.colliders[startIdx + collIdx]
            val relColl: Cloth.Collider = Hand.CLOTH_COLLIDERS[collIdx]
            coll.center = hand.transform
                .transformPosition(Vector3f(relColl.center))
            coll.radius = relColl.radius
        }
    }

    fun update(inVr: Boolean, deltaTime: Float) {
        this.isActive = inVr
        if(!this.isActive) { return }
        this.transform.identity()
            .translate(this.position)
            .rotateTowards(this.direction, this.up)
            .scale(this.scale)
        val cloth: Cloth? = this.cloth
        if(cloth != null) {
            for(attach in Hand.CLOTH_ATTACHMENTS) {
                cloth.nodeAt(attach.x, attach.y).fixedAt = this.transform
                    .transformPosition(Vector3f(attach.offset))
            }
            for(handIdx in 0..<this.hands.size) {
                this.updateClothColliders(cloth, handIdx)
            }
            cloth.update(deltaTime)
        }
    }

    fun renderShadows(renderer: Renderer) {
        renderer.renderShadows(this.model.get(), listOf(this.transform))
        this.cloth?.renderShadows(renderer)
    }

    fun render(renderer: Renderer) {
        renderer.render(this.model.get(), listOf(this.transform))
        this.cloth?.render(renderer)
    }

}