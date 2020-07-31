package xyz.chunkstories.graphics.opengl.systems

import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackMalloc
import org.lwjgl.system.MemoryStack.stackPush
import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.opengl.*
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderProgram
import xyz.chunkstories.graphics.opengl.shaders.bindShaderResources

class OpenglFullscreenQuadDrawer(pass: OpenglPass, dslCode: DrawingSystem.() -> Unit) : OpenglDrawingSystem(pass), FullscreenQuadDrawer {
    override val defines = mutableMapOf<String, String>()

    val backend: OpenglGraphicsBackend
        get() = pass.backend

    override var shader: String = pass.declaration.name

    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding = 0
            stride = 8
            inputRate = InputRate.PER_VERTEX
        }

        attribute {
            binding = 0
            locationName = "vertexIn"
            format = Pair(VertexFormat.FLOAT, 2)
            offset = 0
        }
    }

    val vertexBuffer: OpenglVertexBuffer

    val program: OpenglShaderProgram
    val pipeline: FakePSO

    init {
        dslCode()

        program = backend.shaderFactory.createProgram(shader, ShaderCompilationParameters(defines = defines))
        pipeline = FakePSO(backend, program, pass, vertexInputConfiguration, FaceCullingMode.CULL_BACK)

        val vertices = floatArrayOf(-1.0F, -3.0F, 3.0F, 1.0F, -1.0F, 1.0F)
        vertexBuffer = OpenglVertexBuffer(backend)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun executeDrawingCommands(context: OpenglPassInstance) {
        pipeline.bind()
        pipeline.bindVertexBuffer(0, vertexBuffer)
        context.bindShaderResources(pipeline)

        //GL20.glValidateProgram(pipeline.program.programId)
        //println(GL20.glGetProgramInfoLog(pipeline.program.programId))

        glDrawArrays(GL_TRIANGLES, 0, 3)
        pipeline.unbind()
    }

    override fun cleanup() {
        vertexBuffer.cleanup()
        pipeline.cleanup()
        program.cleanup()
    }
}