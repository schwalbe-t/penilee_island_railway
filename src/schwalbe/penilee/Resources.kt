
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


val SIGNAL_BOX = ObjLoader("res/models/signal_box.obj", Renderer.OBJ_LAYOUT)

val LEVER_BASE = ObjLoader("res/models/lever_base.obj", Renderer.OBJ_LAYOUT)
val LEVER_BODY = ObjLoader("res/models/lever_body.obj", Renderer.OBJ_LAYOUT)
val LEVER_BODY_RED = ImageLoader("res/models/lever_body_red.png")
val LEVER_BODY_BLUE = ImageLoader("res/models/lever_body_blue.png")
val LEVER_BODY_BROWN = ImageLoader("res/models/lever_body_brown.png")
val LEVER_SIGN_NUM_1 = ImageLoader("res/models/lever_sign_1.png")
val LEVER_SIGN_NUM_2 = ImageLoader("res/models/lever_sign_2.png")
val LEVER_SIGN_NUM_3 = ImageLoader("res/models/lever_sign_3.png")
val LEVER_CLUTCH = ObjLoader("res/models/lever_clutch.obj", Renderer.OBJ_LAYOUT)

val HAND_IDLE = ObjLoader("res/models/hand_idle.obj", Renderer.OBJ_LAYOUT)

fun loadAllResources() = loadResources(
    RENDERER_SHADOW_SHADER,
    RENDERER_GEOMETRY_SHADER,
    RENDERER_POST_SHADER,

    SIGNAL_BOX,
    LEVER_BASE, LEVER_BODY, LEVER_CLUTCH,
    LEVER_BODY_RED, LEVER_BODY_BLUE, LEVER_BODY_BROWN,
    LEVER_SIGN_NUM_1, LEVER_SIGN_NUM_2, LEVER_SIGN_NUM_3,
    HAND_IDLE
)