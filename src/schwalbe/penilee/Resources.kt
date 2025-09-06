
package schwalbe.penilee.resources

import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.loadResources
import schwalbe.penilee.Renderer

val RENDERER_SHADOW_SHADER = GlslLoader(
    "res/shaders/geometry.vert.glsl",
    "res/shaders/shadows.frag.glsl"
)
val RENDERER_GEOMETRY_SHADER = GlslLoader(
    "res/shaders/geometry.vert.glsl",
    "res/shaders/geometry.frag.glsl"
)
val RENDERER_POST_SHADER = GlslLoader(
    "res/shaders/post.vert.glsl",
    "res/shaders/post.frag.glsl"
)

val SIGNAL_BOX = ObjLoader(
    "res/models/signal_box.obj",
    Renderer.OBJ_LAYOUT,
    FaceCulling.DISABLED
)

fun loadAllResources() = loadResources(
    RENDERER_SHADOW_SHADER,
    RENDERER_GEOMETRY_SHADER,
    RENDERER_POST_SHADER,

    SIGNAL_BOX
)