package xyz.chunkstories.graphics.opengl

import org.lwjgl.opengl.GL32.*
import org.lwjgl.opengl.ARBDrawBuffersBlend.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL14.GL_FUNC_ADD
import org.lwjgl.opengl.GL14.GL_SRC_ALPHA

import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.graphics.rendergraph.PassOutput

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.opengl.buffers.OpenglUniformBuffer
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.buffers.glVertexFormat
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderProgram

/** OpenGL doesn't have Pipelines/PSOs, instead we emulate this stuff */
class FakePSO(val backend: OpenglGraphicsBackend, val program: OpenglShaderProgram, val pass: OpenglPass,
              val vertexInputConfiguration: VertexInputConfiguration, faceCullingMode: FaceCullingMode) : Cleanable {

    val faceCullingMode = faceCullingMode

    val ubos: Map<GLSLUniformBlock, OpenglUniformBuffer>

    init {
        ubos = program.glslProgram.resources.filterIsInstance<GLSLUniformBlock>().map {
            Pair(it, OpenglUniformBuffer(backend, it.struct))
        }.toMap()
    }

    fun bind() {
        glUseProgram(program.programId)

        // Set cull state
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

        // Set blending state
        for((i, colorAttachement) in pass.declaration.outputs.outputs.withIndex()) {
            when(colorAttachement.blending) {
                PassOutput.BlendMode.OVERWRITE -> {
                    glDisable(GL_BLEND)
                }
                PassOutput.BlendMode.ALPHA_TEST -> {
                    glEnable(GL_BLEND)
                    glBlendEquationiARB(i, GL_FUNC_ADD)
                    glBlendFunciARB(i, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                }
                PassOutput.BlendMode.MIX -> {
                    glEnable(GL_BLEND)
                    glBlendEquationiARB(i, GL_FUNC_ADD)
                    glBlendFunciARB(i, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                }
                PassOutput.BlendMode.ADD -> TODO()
                PassOutput.BlendMode.PREMULTIPLIED_ALPHA -> TODO()
            }
        }



        // Configure vertex pulling
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
        ubos.values.forEach(Cleanable::cleanup)
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