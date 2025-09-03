
package schwalbe.penilee.engine.gfx

import schwalbe.penilee.engine.Resource
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class ImageLoader(val path: String): Resource<Texture2>() {
    override fun load(): Texture2 { 
        MemoryStack.stackPush().use { stack ->
            val widthPtr = stack.mallocInt(1)
            val heightPtr = stack.mallocInt(1)
            val channelsPtr = stack.mallocInt(1)
            stbi_set_flip_vertically_on_load(true)
            val imageBuffer: ByteBuffer? = stbi_load(
                path, widthPtr, heightPtr, channelsPtr, 4
            )
            check(imageBuffer != null) { "Failed to load image ${path}" }
            val texture = Texture2(
                widthPtr.get(0), heightPtr.get(0), imageBuffer
            )
            stbi_image_free(imageBuffer)
            return texture
        }
    }
}