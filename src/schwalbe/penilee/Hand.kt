
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.VrController
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import org.joml.*

class Hand(val controller: VrController, val scale: Vector3fc) {

    class ClothAttachment(val x: Int, val y: Int, val offset: Vector3fc)

    companion object {
        val CLOTH_ORIGIN_OFFSET: Vector3fc = Vector3f(0.01f, 0f, -0.1f)
        val CLOTH_ATTACHMENTS: List<ClothAttachment> = listOf(
            ClothAttachment(0, 0, Vector3f(0.01f, 0f, -0.1f)),
            ClothAttachment(2, 0, Vector3f(0.01f, 0f, -0.15f))
        )
    }


    var cloth: Cloth? = null
        private set

    var isActive: Boolean = false
        private set
    var model: ObjLoader = HAND_IDLE
    var position: Vector3f = Vector3f()
    var direction: Vector3f = Vector3f()
    var up: Vector3f = Vector3f()

    fun addCloth() {
        if(this.cloth != null) { return }
        this.cloth = Cloth(
            origin = Vector3f(this.position).add(Hand.CLOTH_ORIGIN_OFFSET), 
            dirX = this.up, dirY = this.direction,
            width = 32, height = 32, nodeDist = 1f/32f/4f, 
            texture = CLOTH_PATTERN.get(), stiffness = 10000f, mass = 0.1f
        )
    }

    fun removeCloth() {
        this.cloth?.destroy()
        this.cloth = null
    }

    fun update(inVr: Boolean, player: Player, deltaTime: Float) {
        this.isActive = inVr
        if(!this.isActive) { return } 
        this.position = Vector3f(this.controller.gripPos)
            .rotateY(player.angleY)
            .add(player.computeHeadPos())
        this.direction = Vector3f(this.controller.gripDir)
            .rotateY(player.angleY)
        this.up = Vector3f(this.controller.gripUp)
            .rotateY(player.angleY)
        val cloth: Cloth? = this.cloth
        if(cloth != null) {
            for(attach in Hand.CLOTH_ATTACHMENTS) {
                cloth.nodeAt(attach.x, attach.y)
                    .fixedAt = Vector3f(this.position).add(attach.offset)
            }
            cloth.update(deltaTime)
        }
    }

    fun renderShadows(renderer: Renderer) {
        renderer.renderShadows(this.model.get(), listOf(Matrix4f()
            .translate(this.position)
            .rotateTowards(this.direction, this.up)
            .scale(this.scale)
        ))
        this.cloth?.renderShadows(renderer)
    }

    fun render(renderer: Renderer) {
        renderer.render(this.model.get(), listOf(Matrix4f()
            .translate(this.position)
            .rotateTowards(this.direction, this.up)
            .scale(this.scale)
        ))
        this.cloth?.render(renderer)
    }

}