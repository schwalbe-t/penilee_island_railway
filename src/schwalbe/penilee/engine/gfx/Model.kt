
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
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        texOverrides: Map<Pair<String, String>, Texture2> = mapOf()
    ) {
        for((meshId, mesh) in this.meshes) {
            if(localTransfUniform != null) {
                shader.setMatrix4(localTransfUniform, mesh.localTransf)
            }
            if(textureUniform != null) {
                val texture: Texture2 = texOverrides.get(meshId) ?: mesh.texture
                shader.setTexture2(textureUniform, texture)
            }
            mesh.geometry.render(
                shader, dest, instanceCount, this.faceCulling, depthTesting
            )
        }
    }

}