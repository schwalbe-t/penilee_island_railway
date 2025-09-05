
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*

enum class TextureFormat(val fmt: Int, val iFmt: Int, val chType: Int) {
    // Color
    R8       (GL_RED,  GL_R8,           GL_UNSIGNED_BYTE ),
    RG8      (GL_RG,   GL_RG8,          GL_UNSIGNED_BYTE ),
    RGB8     (GL_RGB,  GL_RGB8,         GL_UNSIGNED_BYTE ),
    RGBA8    (GL_RGBA, GL_RGBA8,        GL_UNSIGNED_BYTE ),
    SN_R8    (GL_RED,  GL_R8_SNORM,     GL_BYTE          ),
    SN_RG8   (GL_RG,   GL_RG8_SNORM,    GL_BYTE          ),
    SN_RGB8  (GL_RGB,  GL_RGB8_SNORM,   GL_BYTE          ),
    SN_RGBA8 (GL_RGBA, GL_RGBA8_SNORM,  GL_BYTE          ),
    R16      (GL_RED,  GL_R16,          GL_UNSIGNED_SHORT),
    RG16     (GL_RG,   GL_RG16,         GL_UNSIGNED_SHORT),
    RGB16    (GL_RGB,  GL_RGB16,        GL_UNSIGNED_SHORT),
    RGBA16   (GL_RGBA, GL_RGBA16,       GL_UNSIGNED_SHORT),
    SN_R16   (GL_RED,  GL_R16_SNORM,    GL_SHORT         ),
    SN_RG16  (GL_RG,   GL_RG16_SNORM,   GL_SHORT         ),
    SN_RGB16 (GL_RGB,  GL_RGB16_SNORM,  GL_SHORT         ),
    SN_RGBA16(GL_RGBA, GL_RGBA16_SNORM, GL_SHORT         ),
    // Depth
    DEPTH16  (GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16,  GL_UNSIGNED_SHORT),
    DEPTH24  (GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24,  GL_UNSIGNED_INT  ),
    DEPTH32  (GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT32,  GL_UNSIGNED_INT  ),
    F_DEPTH32(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT32F, GL_FLOAT         ),
    // Depth + Stencil
    DEPTH24_STENCIL8(GL_DEPTH_STENCIL, GL_DEPTH24_STENCIL8,  GL_UNSIGNED_INT_24_8)
}

interface Texture {
    val width: Int
    val height: Int
}