
package schwalbe.penilee

import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.input.*
import org.joml.*
import kotlin.math.abs

class Interaction(val vrGrippingCond: (VrController) -> Boolean) {

    companion object {
        val MAX_MOUSE_WORLD_DIST: Float = 3f
        val MAX_VR_GRIP_DIST: Float = 0.125f
        val MAX_WORLD_DIST: Float = 3f
    }


    var position: Vector3fc = Vector3f()
    var isClosest: Boolean = false
    var gripping: Hand? = null
    var locked: Boolean = false


    class Manager(var interactions: List<Interaction>) {

        fun grippedByHand(hand: Hand, hands: List<Hand>): Interaction? {
            if(!hand.isActive) { return null }
            var closest: Interaction? = null
            var closestDist: Float = Float.POSITIVE_INFINITY
            for(ia in this.interactions) {
                if(!ia.vrGrippingCond(hand.controller)) { continue }
                val dist: Float = ia.position.distance(hand.position)
                if(dist > Interaction.MAX_VR_GRIP_DIST) { continue }
                if(dist >= closestDist) { continue }
                closestDist = dist
                closest = ia
            }
            if(closest == null) { return null }
            closest.gripping = hand
            if(hand.cloth == null) {
                hands.forEach(Hand::removeCloth)
                hand.addCloth()
            }
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

        fun update(
            player: Player, camera: Camera, hands: List<Hand>, inVr: Boolean
        ) {
            val camPos: Vector3f = camera.computePos()
            val anyLocked: Boolean = this.interactions.any {
                val dist: Float = camPos.distance(it.position)
                if(dist > Interaction.MAX_WORLD_DIST) { it.locked = false }
                it.locked
            }
            if(anyLocked) { return }
            this.interactions.forEach {
                it.isClosest = false
                it.gripping = null
            }
            var closest: Interaction? = null
            for(hand in hands) {
                if(closest != null) { break }
                closest = this.grippedByHand(hand, hands)
            }
            if(closest == null && !inVr) {
                closest = this.closestToMouse(camPos, camera)
            }
            closest?.isClosest = true
        }
    
    }

}