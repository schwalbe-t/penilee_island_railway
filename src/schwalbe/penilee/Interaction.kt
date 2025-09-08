
package schwalbe.penilee

import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.input.*
import org.joml.*
import kotlin.math.abs

class Interaction(val vrGrippingCond: (VrController) -> Boolean) {

    companion object {
        val MAX_MOUSE_WORLD_DIST: Float = 3f
        val MAX_VR_GRIP_DIST: Float = 0.5f
    }


    var position: Vector3fc = Vector3f()
    var isClosest: Boolean = false
    var gripping: VrController? = null
    var locked: Boolean = false


    class Manager(var interactions: List<Interaction>) {

        fun grippedByController(
            player: Player, controller: VrController
        ): Interaction? {
            val controllerPos: Vector3fc = Vector3f(controller.gripPos)
                .rotateY(player.angleY)
                .add(player.computeHeadPos())
            var closest: Interaction? = null
            var closestDist: Float = Float.POSITIVE_INFINITY
            for(ia in this.interactions) {
                if(!ia.vrGrippingCond(controller)) { continue }
                val dist: Float = ia.position.distance(controllerPos)
                if(dist > Interaction.MAX_VR_GRIP_DIST) { continue }
                if(dist >= closestDist) { continue }
                closestDist = dist
                closest = ia
            }
            closest?.gripping = controller
            return closest
        }

        fun closestToMouse(camPos: Vector3fc, camera: Camera): Interaction? {
            var closest: Interaction? = null
            var closestDist: Float = Float.POSITIVE_INFINITY
            for(ia in this.interactions) {
                val worldDist: Float = ia.position.distance(camPos)
                if(worldDist > Interaction.MAX_MOUSE_WORLD_DIST) { continue }
                val viewPos: Vector3f = camera.worldToView(ia.position)
                // camera looks down -Z, so anything with Z > 0 is not visible
                if(viewPos.z > 0f) { continue }
                val ndcPos: Vector3f = camera.viewToNdc(viewPos)
                // this will ignore the Y axis and only find the closest
                // interaction based on the horizontal distance to the center
                // of the screen
                val screenDist: Float = abs(ndcPos.x())
                if(screenDist >= closestDist) { continue }
                closestDist = screenDist
                closest = ia
            }
            return closest
        }

        fun update(player: Player, camera: Camera, inVr: Boolean) {
            if(this.interactions.any { it.locked }) { return }
            this.interactions.forEach {
                it.isClosest = false
                it.gripping = null
            }
            val closestVr: Interaction? = if(!inVr) null
                else this.grippedByController(player, VrController.LEFT)
                ?: this.grippedByController(player, VrController.RIGHT)
            val closestNonVr: Interaction? = if(inVr) null
                else this.closestToMouse(camera.computePos(), camera)
            val closest = closestVr ?: closestNonVr
            closest?.isClosest = true
        }
    
    }

}