
package schwalbe.penilee.engine

import kotlin.math.PI
import org.joml.*

val Int.degrees: Float
    get() = this.toFloat() * (2f * PI.toFloat() / 360f)

val Float.degrees: Float
    get() = this * (2f * PI.toFloat() / 360f)

val Double.degrees: Double
    get() = this * (2.0 * PI / 360.0)

val ORIGIN: Vector3f = Vector3f(0f, 0f, 0f)
val UP: Vector3f = Vector3f(0f, 1f, 0f)
val NDC_INTO_SCREEN: Vector3f = Vector3f(0f, 0f, -1f)