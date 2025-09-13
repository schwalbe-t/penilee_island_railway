
package schwalbe.penilee.level

import schwalbe.penilee.*
import schwalbe.penilee.network.*
import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.*
import schwalbe.penilee.engine.gfx.*
import schwalbe.penilee.resources.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import org.joml.*

class Level(
    val ground: Model, val hasWater: Boolean,
    val layout: Layout, val signalBox: SignalBox
) {

    @Serializable
    class Serialized(
        val name: String,
        val ground: String,
        val hasWater: Boolean,
        val layout: String,
        val signalBox: SignalBox.Serialized,
        val renderer: Level.RendererConfig
    ) {
        fun toLevel(r: Renderer): Level {
            val groundP: String = "res/" + this.ground
            val groundM: Model = ObjLoader(groundP, Renderer.OBJ_LAYOUT).load()
            val layoutR: String = "res/" + this.layout
            val layoutP: Layout = parseTrackLayout(File(layoutR).readText())
            val signalBox: SignalBox = this.signalBox.toSignalBox(
                layoutP.createLevers()
            )
            this.renderer.applyTo(r)
            return Level(groundM, this.hasWater, layoutP, signalBox)
        }
    }

    @Serializable
    class RendererConfig(
        val background: List<Int>,
        val suns: List<Level.RendererSunConfig>,
        val lights: List<Level.RendererLightConfig>,
        val shadowMapRes: Int,
        val depthBias: Float,
        val normalOffset: Float,
        val outsideShadowMapLit: Boolean,
        val sunDir: List<Float>
    ) {
        fun applyTo(renderer: Renderer) {
            renderer.background = Vector3f(
                this.background[0].toFloat() / 255f,
                this.background[1].toFloat() / 255f,
                this.background[2].toFloat() / 255f
            )
            val sunLights: List<Matrix4fc> = this.suns
                .map(RendererSunConfig::toLight)
            val lightLights: List<Matrix4fc> = this.lights
                .map(RendererLightConfig::toLight)
            renderer.configureLighting(
                lights = sunLights + lightLights,
                shadowMapRes = this.shadowMapRes,
                depthBias = this.depthBias,
                normalOffset = this.normalOffset,
                outsideShadowMapLit = this.outsideShadowMapLit,
                sunDirection = Vector3f(
                    this.sunDir[0], this.sunDir[1], this.sunDir[2]
                )
            )
        }
    }

    @Serializable
    class RendererSunConfig(
        val dir: List<Float>,
        val target: List<Float>,
        val up: List<Float>,
        val radius: Float,
        val dist: Float
    ) {
        fun toLight(): Matrix4fc = Light.sunAlongTowards(
            direction = Vector3f(this.dir[0], this.dir[1], this.dir[2]),
            towards = Vector3f(this.target[0], this.target[1], this.target[2]),
            up = Vector3f(this.up[0], this.up[1], this.up[2]),
            radius = this.radius,
            distance = this.dist          
        )
    }

    @Serializable
    class RendererLightConfig(
        val pos: List<Float>,
        val dir: List<Float>,
        val up: List<Float>,
        val angle: Float,
        val dist: Float
    ) {
        fun toLight(): Matrix4fc = Light.fromAlong(
            from = Vector3f(this.pos[0], this.pos[1], this.pos[2]),
            direction = Vector3f(this.dir[0], this.dir[1], this.dir[2]),
            up = Vector3f(this.up[0], this.up[1], this.up[2]),
            angle = this.angle.degrees,
            distance = this.dist
        )
    }

    val camera = Camera()
    val player = Player(
        this.signalBox.transform.transformPosition(Vector3f(0f, 0f, 0f)), 
        this.signalBox.computeBounds()
    )
    val hands = listOf(
        Hand(VrController.LEFT, Vector3f(-1f, 1f, 1f)), 
        Hand(VrController.RIGHT, Vector3f(1f, 1f, 1f))
    )
    val interactions = Interaction.Manager(this.signalBox.collectInteractions())
    
    init {
        this.hands.forEach { it.hands = this.hands }
    }

    fun update(deltaTime: Float, inVr: Boolean, windowCam: Camera) {
        this.player.update(deltaTime, inVr, windowCam)
        this.player.configureCamera(this.camera)
        
        this.hands.forEach { it.inheritPosition(this.player) }
        this.interactions.update(this.player, windowCam, this.hands, inVr)
        this.signalBox.update(deltaTime, this.player, windowCam)
        this.layout.update(deltaTime)
        this.hands.forEach { it.update(inVr, deltaTime) }
    }

    fun renderShadows(renderer: Renderer, deltaTime: Float) {
        this.hands.forEach { it.renderShadows(renderer) }
        this.signalBox.renderShadows(renderer)
        this.layout.renderShadows(renderer)
    }

    fun render(renderer: Renderer, screenCam: Camera, deltaTime: Float) {
        screenCam.parent = this.camera
        renderer.applyCamera(screenCam)

        this.hands.forEach { it.render(renderer) }
        this.signalBox.render(renderer)
        this.layout.render(renderer)
    }

}


val LEVEL_LIST_PATH: String = "res/levels/levellist.json"

fun loadLevelConfigs(): List<Level.Serialized> {
    val levelListR: String = File(LEVEL_LIST_PATH).readText()
    val levelList = Json.decodeFromString<List<String>>(levelListR)
    return levelList.map { configPath ->
        val configR: String = File("res/" + configPath).readText()
        Json.decodeFromString<Level.Serialized>(configR) 
    }
}