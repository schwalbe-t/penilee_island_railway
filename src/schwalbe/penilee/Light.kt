
package schwalbe.penilee

import schwalbe.penilee.engine.UP
import org.joml.*

class Light {
    companion object {
        val NEAR_PLANE: Float = 1f;
    }
}


fun Light.Companion.fromTowards(
    from: Vector3f, towards: Vector3f, up: Vector3f = UP,
    angle: Float, distanceFactor: Float = 2f
): Matrix4f = Light.fromAlong(
    from = from,
    direction = Vector3f(towards).sub(from),
    up = up,
    angle = angle,
    distance = from.distance(towards) * distanceFactor
)

fun Light.Companion.alongTowards(
    direction: Vector3f, towards: Vector3f, up: Vector3f = UP,
    angle: Float, distance: Float
): Matrix4f = Light.fromAlong(
    from = Vector3f(direction).normalize().mul(distance).negate().add(towards),
    direction = direction,
    up = up,
    angle = angle,
    distance = distance
)

fun Light.Companion.fromAlong(
    from: Vector3f, direction: Vector3f, up: Vector3f = UP,
    angle: Float, distance: Float
): Matrix4f = Matrix4f()
    .perspective(angle, 1f, Light.NEAR_PLANE, distance)
    .lookAt(from, Vector3f(from).add(direction), up)


fun Light.Companion.sunFromTowards(
    from: Vector3f, towards: Vector3f, up: Vector3f = UP,
    radius: Float, distanceFactor: Float = 2f
): Matrix4f = Light.sunFromAlong(
    from = from,
    direction = Vector3f(towards).sub(from),
    up = up,
    radius = radius,
    distance = from.distance(towards) * distanceFactor
)

fun Light.Companion.sunAlongTowards(
    direction: Vector3f, towards: Vector3f, up: Vector3f = UP,
    radius: Float, distance: Float
): Matrix4f = Light.sunFromAlong(
    from = Vector3f(direction).normalize().mul(distance).negate().add(towards),
    direction = direction,
    up = up,
    radius = radius,
    distance = distance
)

fun Light.Companion.sunFromAlong(
    from: Vector3f, direction: Vector3f, up: Vector3f = UP,
    radius: Float, distance: Float
): Matrix4f = Matrix4f()
    .ortho(-radius, radius, -radius, radius, Light.NEAR_PLANE, distance)
    .lookAt(from, Vector3f(from).add(direction), up)