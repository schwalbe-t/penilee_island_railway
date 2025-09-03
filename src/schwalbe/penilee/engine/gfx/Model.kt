
package schwalbe.penilee.engine.gfx

import org.joml.*

class Model(
    val meshes: Map<Pair<String, String>, Mesh>,
    val faceCulling: FaceCulling = FaceCulling.DISABLED
) {

    class Mesh(
        val geometry: Geometry, 
        val texture: Texture2, 
        val localTransf: Matrix4f
    )

    fun render(
        shader: Shader, dest: Framebuffer,
        instanceCount: Int = 1,
        localTransfUniform: String? = null,
        textureUniform: String? = null,
        depthTesting: DepthTesting = DepthTesting.ENABLED
    ) {
        for(mesh in this.meshes.values) {
            if(localTransfUniform != null) {
                shader.setMatrix4(localTransfUniform, mesh.localTransf)
            }
            if(textureUniform != null) {
                shader.setTexture2(textureUniform, mesh.texture)
            }
            mesh.geometry.render(
                shader, dest, instanceCount, this.faceCulling, depthTesting
            )
        }
    }

}