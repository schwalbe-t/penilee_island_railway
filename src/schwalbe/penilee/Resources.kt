
package schwalbe.penilee.resources

import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.engine.loadResources

val TEST_SHADER = GlslLoader(
    "res/shaders/test_vert.glsl", "res/shaders/test_frag.glsl"
)

val SIGNAL_BOX = ObjLoader(
    "res/models/signal_box.obj",
    listOf(ObjAttrib.POSITION, ObjAttrib.TEX_COORDS),
    FaceCulling.BACK
)

fun loadAllResources() = loadResources(
    TEST_SHADER,

    SIGNAL_BOX
)