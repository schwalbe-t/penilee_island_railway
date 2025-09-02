
package schwalbe.penilee.engine

abstract class Resource<T> {

    var loaded: T? = null

    abstract fun load(): T

    fun cachedLoad() {
        if(this.loaded != null) { return }
        this.loaded = this.load()
    }

    fun get(): T = this.loaded
        ?: throw IllegalStateException("Resource has not yet been loaded") 

}

fun loadResources(vararg res: Resource<*>)
    = res.forEach { it.cachedLoad() }