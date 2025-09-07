
package schwalbe.penilee.engine

import org.joml.*
import kotlin.math.*

class AABB(start: Vector3fc, end: Vector3fc) {

    val start: Vector3fc = Vector3f(
        min(start.x(), end.x()),
        min(start.y(), end.y()),
        min(start.z(), end.z())
    )
    val end: Vector3fc = Vector3f(
        max(start.x(), end.x()),
        max(start.y(), end.y()),
        max(start.z(), end.z())
    )

    fun isInside(p: Vector3fc): Boolean
        = this.start.x() <= p.x() && p.x() <= this.end.x()
        && this.start.y() <= p.y() && p.y() <= this.end.y()
        && this.start.z() <= p.z() && p.z() <= this.end.z()

    fun insideClosestTo(p: Vector3fc): Vector3f = Vector3f(
        p.x().coerceIn(this.start.x(), this.end.x()),
        p.y().coerceIn(this.start.y(), this.end.y()),
        p.z().coerceIn(this.start.z(), this.end.z())
    )

}