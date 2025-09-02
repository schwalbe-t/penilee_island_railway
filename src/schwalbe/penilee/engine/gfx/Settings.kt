
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*

enum class DepthTesting {
    DISABLED { 
        override fun use() = glDisable(GL_DEPTH_TEST)
    },
    ENABLED { 
        override fun use() = glEnable(GL_DEPTH_TEST)
    };

    abstract fun use()
}

enum class FaceCulling {
    DISABLED { 
        override fun use() = glDisable(GL_CULL_FACE)
    },
    BACK {
        override fun use() {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_BACK)
        }
    }, 
    FRONT {
        override fun use() {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_FRONT)
        }
    }, 
    FRONT_AND_BACK {
        override fun use() {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_FRONT_AND_BACK)
        }
    };

    abstract fun use()
}