
package schwalbe.penilee.engine.gfx

class Texture {

    val id: Int
    val width: Int
    val height: Int
    val layers: Int
    val owning: Boolean

    constructor(id: Int, width: Int, height: Int, layers: Int = 1, owning: Boolean = false) {
        this.id = id
        this.width = width
        this.height = height
        this.layers = layers
        this.owning = owning
    }

    constructor(width: Int, height: Int, layers: Int = 1) {
        this.id = 0 // TODO! not yet implemented
        this.width = width
        this.height = height
        this.layers = layers
        this.owning = true
    }

    fun resizeFast(width: Int, height: Int) {
        // TODO! not yet implemented
    }

    fun destroy() {
        if(!this.owning) { return }
        // TODO! not yet implemented
    }

}