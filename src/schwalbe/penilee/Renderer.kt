
package schwalbe.penilee

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.RENDERER_SHADOW_SHADER
import schwalbe.penilee.resources.RENDERER_GEOMETRY_SHADER
import schwalbe.penilee.resources.RENDERER_POST_SHADER
import org.joml.*
import kotlin.math.min

class Renderer {

    companion object {
        val VERTEX_LAYOUT: VertexLayout = listOf(
            Pair(3, Geometry.Type.FLOAT),
            Pair(2, Geometry.Type.FLOAT),
            Pair(3, Geometry.Type.FLOAT)
        )
        
        val OBJ_LAYOUT: List<ObjAttrib> = listOf(
            ObjAttrib.POSITION,
            ObjAttrib.TEX_COORDS,
            ObjAttrib.NORMAL
        )

        private val INSTANCES_PER_CALL: Int = 128
        val MAX_LIGHTS: Int = 8

        private val OUTPUT_COLOR_FMT = TextureFormat.RGBA8
        private val OUTPUT_DEPTH_FMT = TextureFormat.DEPTH24
        private val SHADOW_MAP_DEPTH_FMT = TextureFormat.DEPTH24
    }

    private val blitGeometry: Geometry
    private val output = Framebuffer()
    private var outputColor: Texture2? = null
    private var outputDepth: Texture2? = null
    private var shadowMaps = Texture3(10, 10, 1, Renderer.SHADOW_MAP_DEPTH_FMT)
    private var shadowMapDest = Framebuffer()
    private var lightViewProj: List<Matrix4fc> = listOf()
    var background = Vector3f(141f, 183f, 255f).div(255f)

    init {
        val blitGB = Geometry.Builder(listOf(
            Pair(2, Geometry.Type.FLOAT), // ndc
            Pair(2, Geometry.Type.FLOAT)  // uv
        ))
        val a = blitGB.putVertex { it.putFloats(-1f,  1f).putFloats(0f, 1f) }
        val b = blitGB.putVertex { it.putFloats( 1f,  1f).putFloats(1f, 1f) }
        val c = blitGB.putVertex { it.putFloats(-1f, -1f).putFloats(0f, 0f) }
        val d = blitGB.putVertex { it.putFloats( 1f, -1f).putFloats(1f, 0f) }
        blitGB.putElement(a, c, d)
        blitGB.putElement(a, d, b)
        this.blitGeometry = blitGB.build()
    }

    fun configureLighting(
        lights: List<Matrix4fc>, 
        shadowMapRes: Int,
        depthBias: Float, normalOffset: Float, 
        outOfBoundsLit: Boolean, sunDirection: Vector3f
    ) {
        check(lights.size <= Renderer.MAX_LIGHTS) {
            "Renderer only allows rendering ${Renderer.MAX_LIGHTS} light(s)"
        }
        val shadowMapInvalid: Boolean = this.shadowMaps.layers != lights.size
            || this.shadowMaps.width != shadowMapRes
            || this.shadowMaps.height != shadowMapRes
        if(shadowMapInvalid) {
            this.shadowMaps.destroy()
            this.shadowMaps = Texture3(
                shadowMapRes, shadowMapRes, lights.size, 
                Renderer.SHADOW_MAP_DEPTH_FMT
            )
        }
        RENDERER_GEOMETRY_SHADER.get()
            .setInt("uLightCount", lights.size)
            .setMatrix4Arr("uLightViewProj", lights)
            .setTexture3("uShadowMaps", this.shadowMaps)
            .setFloat("uShadowDepthBias", depthBias)
            .setFloat("uShadowNormalOffset", normalOffset)
            .setInt("uOutOfBoundsLit", if(outOfBoundsLit) 1 else 0)
            .setVec3("uSunDirection", sunDirection)
        this.lightViewProj = lights
    }


    fun clearShadows() {
        for(light in 0..<this.shadowMaps.layers) {
            this.shadowMapDest.attachDepth(this.shadowMaps, light)
            this.shadowMapDest.clearDepth(1f)
        }
    }

    fun renderShadows(
        model: Model, modelTransfs: List<Matrix4fc>,
        texOverrides: Map<Pair<String, String>, Texture2> = mapOf()
    ) {
        val shader: Shader = RENDERER_SHADOW_SHADER.get()
        var rendered: Int = 0
        while(rendered < modelTransfs.size) {
            val remaining: Int = modelTransfs.size - rendered
            val callSize: Int = min(remaining, Renderer.INSTANCES_PER_CALL)
            val callModelTransfs: List<Matrix4fc> = modelTransfs
                .slice(rendered..<rendered + callSize)
            shader.setMatrix4Arr("uModelTransfs", callModelTransfs)
            for(light in 0..<this.shadowMaps.layers) {
                this.shadowMapDest.attachDepth(this.shadowMaps, light)
                shader.setMatrix4("uViewProj", this.lightViewProj[light])
                model.render(
                    shader, this.shadowMapDest, callSize,
                    "uLocalTransf", "uTexture", 
                    DepthTesting.ENABLED, texOverrides
                )
            }
            rendered += callSize
        }
    }


    fun beginRender(dest: Framebuffer) {
        val destTex: Texture = (dest.color ?: dest.depth)!!
        val outputTex: Texture? = this.outputColor ?: this.outputDepth
        val outputInvalid: Boolean = outputTex == null
            || outputTex.width != destTex.width
            || outputTex.height != destTex.height
        if(outputInvalid) {
            val oldColorTex: Texture2? = this.outputColor
            this.output.detachColor()
            oldColorTex?.destroy()
            val oldDepthTex: Texture2? = this.outputDepth
            this.output.detachDepth()
            oldDepthTex?.destroy()
            val newColorTex = Texture2(
                destTex.width, destTex.height, Renderer.OUTPUT_COLOR_FMT
            )
            this.outputColor = newColorTex
            this.output.attachColor(newColorTex)
            val newDepthTex = Texture2(
                destTex.width, destTex.height, Renderer.OUTPUT_DEPTH_FMT
            )
            this.outputDepth = newDepthTex
            this.output.attachDepth(newDepthTex)
        }
        this.output.clearColor(Vector4f(this.background, 1.0f))
        this.output.clearDepth(1f)
    }

    fun applyCamera(camera: Camera) {
        RENDERER_GEOMETRY_SHADER.get()
            .setMatrix4("uViewProj", camera.computeViewProj())
    }

    fun render(
        model: Model, modelTransfs: List<Matrix4fc>,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        texOverrides: Map<Pair<String, String>, Texture2> = mapOf()
    ) {
        val shader: Shader = RENDERER_GEOMETRY_SHADER.get()
        var rendered: Int = 0
        while(rendered < modelTransfs.size) {
            val remaining: Int = modelTransfs.size - rendered
            val callSize: Int = min(remaining, Renderer.INSTANCES_PER_CALL)
            val callModelTransfs: List<Matrix4fc> = modelTransfs
                .slice(rendered..<rendered + callSize)
            shader.setMatrix4Arr("uModelTransfs", callModelTransfs)
            model.render(
                shader, this.output, callSize,
                "uLocalTransf", "uTexture", 
                depthTesting, texOverrides
            )
            rendered += callSize
        }
    }

    fun displayRender(dest: Framebuffer) {
        val outputColorTex: Texture2? = this.outputColor
        val outputDepthTex: Texture2? = this.outputDepth
        if(outputColorTex == null || outputDepthTex == null) { return }
        val shader: Shader = RENDERER_POST_SHADER.get()
        shader.setTexture2("uRenderColor", outputColorTex)
        shader.setTexture2("uRenderDepth", outputDepthTex)
        shader.setVec2("uRenderSize", Vector2f(
            outputColorTex.width.toFloat(), outputColorTex.height.toFloat()
        ))
        this.blitGeometry.render(
            shader, dest, 1, FaceCulling.DISABLED, DepthTesting.DISABLED
        )
    }

    fun destroy() {
        this.blitGeometry.destroy()
        this.outputColor?.destroy()
        this.outputDepth?.destroy()
        this.shadowMaps.destroy()
    }

}