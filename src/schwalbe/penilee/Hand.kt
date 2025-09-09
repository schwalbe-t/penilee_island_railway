
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.VrController
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.HAND_IDLE
import org.joml.*

class Hand(val controller: VrController, val scale: Vector3fc) {

    var isActive: Boolean = false
        private set
    var model: ObjLoader = HAND_IDLE
    var position: Vector3f = Vector3f()
    var direction: Vector3f = Vector3f()
    var up: Vector3f = Vector3f()

    fun update(inVr: Boolean, player: Player) {
        this.isActive = inVr
        if(!this.isActive) { return } 
        this.position = Vector3f(this.controller.gripPos)
            .rotateY(player.angleY)
            .add(player.computeHeadPos())
        this.direction = Vector3f(this.controller.gripDir)
            .rotateY(player.angleY)
        this.up = Vector3f(this.controller.gripUp)
            .rotateY(player.angleY)
    }

    fun renderShadows(renderer: Renderer) {
        renderer.renderShadows(this.model.get(), listOf(Matrix4f()
            .translate(this.position)
            .rotateTowards(this.direction, this.up)
            .scale(this.scale)
        ))
    }

    fun render(renderer: Renderer) {
        renderer.render(this.model.get(), listOf(Matrix4f()
            .translate(this.position)
            .rotateTowards(this.direction, this.up)
            .scale(this.scale)
        ))
    }

}