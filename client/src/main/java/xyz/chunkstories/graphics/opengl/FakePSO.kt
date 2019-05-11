package xyz.chunkstories.graphics.opengl

import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL32.*
import xyz.chunkstories.api.graphics.VertexFormat

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.buffers.glVertexFormat
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderProgram

/** OpenGL doesn't have Pipelines/PSOs, instead we emulate this stuff */
class FakePSO(val backend: OpenglGraphicsBackend, val program: OpenglShaderProgram, val pass: OpenglPass,
              val vertexInputConfiguration: VertexInputConfiguration, faceCullingMode: FaceCullingMode) : Cleanable {

    val faceCullingMode = faceCullingMode


    init {
    }

    fun bind() {
        glUseProgram(program.programId)

        when (faceCullingMode) {
            FaceCullingMode.DISABLED -> glDisable(GL_CULL_FACE)
            FaceCullingMode.CULL_FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT)
            }
            FaceCullingMode.CULL_BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK)
            }
        }

        for (vertexInput in program.glslProgram.vertexInputs) {
            glEnableVertexAttribArray(vertexInput.location)
        }
    }

    fun unbind() {
        for (vertexInput in program.glslProgram.vertexInputs) {
            glDisableVertexAttribArray(vertexInput.location)
        }
    }

    fun bindVertexBuffer(slot: Int, vertexBuffer: OpenglVertexBuffer){
        val configuredVertexInput = vertexInputConfiguration.inputBindings.find {
            it.binding == slot
        } ?: return

        // Binds the VBO
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer.glId)

        // Setups all the attributes referencing that VBO
        for(attribute in vertexInputConfiguration.attributes) {
            if(attribute.binding != slot)
                continue

            val vertexInput = program.glslProgram.vertexInputs.find { it.name == attribute.locationName }!!
            val attributeLocation = vertexInput.location

            val glVertexFormat = attribute.format.first.glVertexFormat
            val componentsCount = attribute.format.second

            glVertexAttribPointer(attributeLocation, componentsCount, glVertexFormat.type, glVertexFormat.normalized, configuredVertexInput.stride, attribute.offset.toLong())
        }
    }

    override fun cleanup() {
    }
}

fun vertexInputConfiguration(declaration: VertexInputConfigurationContext.() -> Unit): VertexInputConfiguration {
    val inputBindings = mutableListOf<VertexInputBindingDescription>()
    val attributes = mutableListOf<VertexAttributeDescription>()

    val o = object : VertexInputConfigurationContext {
        override fun binding(decl: VertexInputBindingDescription.() -> Unit) {
            inputBindings += VertexInputBindingDescription().also(decl)
        }

        override fun attribute(decl: VertexAttributeDescription.() -> Unit) {
            attributes += VertexAttributeDescription().also(decl)
        }
    }
    o.apply(declaration)

    return VertexInputConfiguration(inputBindings, attributes)
}

interface VertexInputConfigurationContext {
    fun binding(decl: VertexInputBindingDescription.() -> Unit)

    fun attribute(decl: VertexAttributeDescription.() -> Unit)
}

class VertexInputBindingDescription {
    var binding: Int = -1
    var stride: Int = -1
    lateinit var inputRate: InputRate
}

enum class InputRate {
    PER_VERTEX,
    PER_INSTANCE
}

class VertexAttributeDescription {
    var binding: Int = -1
    lateinit var locationName: String
    lateinit var format: Pair<VertexFormat, Int>
    var offset: Int = 0
}

data class VertexInputConfiguration(
        val inputBindings: List<VertexInputBindingDescription>,
        val attributes: List<VertexAttributeDescription>)