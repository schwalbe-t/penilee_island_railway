
package schwalbe.penilee.engine.gfx

import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

typealias VertexLayout = List<Pair<Int, Geometry.Type>>

class Geometry {

    enum class Type(
        val byteSize: Int, val glType: Int, val isIntType: Boolean
    ) {
        BYTE(1, GL_BYTE, true), 
        U_BYTE(1, GL_UNSIGNED_BYTE, true),
        SHORT(2, GL_SHORT, true), 
        U_SHORT(2, GL_UNSIGNED_SHORT, true),
        INT(4, GL_INT, true),
        U_INT(4, GL_UNSIGNED_INT, true),
        FLOAT(4, GL_FLOAT, false)
    }


    var vaoId: Int = 0
        private set
    var vboId: Int = 0
        private set
    var eboId: Int = 0
        private set
    val indexCount: Int

    constructor(
        layout: VertexLayout, stride: Int, vbo: ByteBuffer, ebo: ShortBuffer,
        indexCount: Int
    ) {
        this.indexCount = indexCount
        this.vaoId = glGenVertexArrays()
        glBindVertexArray(this.vaoId)
        this.vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, this.vboId)
        glBufferData(GL_ARRAY_BUFFER, vbo, GL_STATIC_DRAW)
        this.eboId = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.eboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ebo, GL_STATIC_DRAW)
        var byteOffset: Long = 0L
        for(idx in 0..<layout.size) {
            val (count, type) = layout[idx]
            glEnableVertexAttribArray(idx)
            if(type.isIntType) {
                glVertexAttribIPointer(
                    idx, count, type.glType, stride, byteOffset
                )
            } else {
                glVertexAttribPointer(
                    idx, count, type.glType, false, stride, byteOffset
                )
            }
            byteOffset += count * type.byteSize
        }
        glBindVertexArray(0)
    }

    fun render(
        shader: Shader, dest: Framebuffer,
        instanceCount: Int = 1,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED
    ) {
        faceCulling.use()
        depthTesting.use()
        shader.bind()
        dest.bind()
        glBindVertexArray(this.vaoId)
        if(instanceCount > 1) {
            glDrawElementsInstanced(
                GL_TRIANGLES, this.indexCount, GL_UNSIGNED_SHORT, 0, 
                instanceCount
            )
        } else {
            glDrawElements(
                GL_TRIANGLES, this.indexCount, GL_UNSIGNED_SHORT, 0
            )
        }
        glBindVertexArray(0)
    }

    fun destroy() {
        if(this.vaoId != 0) { glDeleteVertexArrays(this.vaoId) }
        this.vaoId = 0
        if(this.vboId != 0) { glDeleteBuffers(this.vboId) }
        this.vboId = 0
        if(this.eboId != 0) { glDeleteBuffers(this.eboId) }
        this.eboId = 0
    }


    class Builder(val layout: VertexLayout) {

        val stride: Int = layout.sumOf { (n, type) -> n * type.byteSize }

        val vertices = mutableListOf<ByteBuffer>()
        val elements = mutableListOf<UShort>()

        class VertexBuilder(stride: Int) {
            val buffer = ByteBuffer
                .allocateDirect(stride)
                .order(ByteOrder.nativeOrder())

            fun putBytes(vararg values: Byte): VertexBuilder {
                this.buffer.put(values)
                return this
            }

            fun putUBytes(vararg values: UByte): VertexBuilder {
                values.forEach { this.buffer.put(it.toByte()) }
                return this
            }

            fun putShorts(vararg values: Short): VertexBuilder {
                values.forEach { this.buffer.putShort(it) }
                return this
            }

            fun putUShorts(vararg values: UShort): VertexBuilder {
                values.forEach { this.buffer.putShort(it.toShort()) }
                return this
            }

            fun putInts(vararg values: Int): VertexBuilder {
                values.forEach { this.buffer.putInt(it) }
                return this
            }

            fun putUInts(vararg values: UInt): VertexBuilder {
                values.forEach { this.buffer.putInt(it.toInt()) }
                return this
            }

            fun putFloats(vararg values: Float): VertexBuilder {
                values.forEach { this.buffer.putFloat(it) }
                return this
            }
        }

        fun putVertex(f: (VertexBuilder) -> Unit): UShort {
            val vertex = VertexBuilder(this.stride)
            f(vertex)
            vertex.buffer.flip()
            val vertexI = this.vertices.size
            this.vertices.add(vertex.buffer)
            return vertexI.toUShort()
        }

        fun putElement(a: UShort, b: UShort, c: UShort) {
            this.elements.add(a)
            this.elements.add(b)
            this.elements.add(c)
        }

        fun build(): Geometry {
            val vbo: ByteBuffer = ByteBuffer
                .allocateDirect(this.vertices.size * this.stride)
                .order(ByteOrder.nativeOrder())
            this.vertices.forEach { vbo.put(it) }
            this.vertices.clear()
            vbo.flip()
            val ebo: ShortBuffer = ByteBuffer
                .allocateDirect(this.elements.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            this.elements.forEach { ebo.put(it.toShort()) }
            val indexCount: Int = this.elements.size
            this.elements.clear()
            ebo.flip()
            return Geometry(this.layout, this.stride, vbo, ebo, indexCount)
        }

    }

}