
package schwalbe.penilee.network

import schwalbe.penilee.*
import schwalbe.penilee.engine.*
import org.joml.*

class Layout(
    val graph: Graph,
    val levers: List<LeverHandler>,
    val ebis: List<EbiHandler>,
    val bells: List<BellHandler>
) {

    class LeverHandler(
        val type: LeverHandler.Type,
        val isLocked: () -> LeverHandler.LockReason?, 
        val onChange: (Boolean) -> Unit
    ) {
        enum class Type { SIGNAL, POINT, CROSSING }

        class LockReason(
            val type: LockReason.Type,
            val context: String
        ) {
            enum class Type {
                // signals
                POINT_MISALIGNED,         // context = <the point>
                OPPOSITE_SIGNAL_ENTRY,    // context = <the wrong-side signal>
                NEXT_BLOCKS_OCCUPIED,     // context = <the train>
                ADVERSE_SIGNAL_CHANGE,    // context = <the train>
                CROSSING_OPEN,            // context = <the crossing>
                // points
                USED_IN_PATH,             // context = <the entry signal>
                // level crossing
                CROSSING_BLOCKS_OCCUPIED  // context = <the train>
            }
        }
    }

    class EbiHandler(
        val isClear: () -> Boolean
    )

    class BellHandler(
        val onPress: () -> Unit,
        val getOnRing: () -> () -> Unit
    )

    fun update(deltaTime: Float) {
        // TODO!
    }

    fun renderShadows(renderer: Renderer) {
        this.graph.renderShadows(renderer)
    }

    fun render(renderer: Renderer) {
        this.graph.render(renderer)
    }

}