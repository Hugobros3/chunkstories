package xyz.chunkstories.graphics.opengl.shaders

import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL32.*
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

class OpenglShaderProgram(val backend: OpenglGraphicsBackend, val glslProgram: GLSLProgram) : Cleanable {
    internal val programId: Int
    private val stages: List<Int>

    val uboIndexes: Map<GLSLUniformBlock, Int>
    val uboBindings: Map<GLSLUniformBlock, Int>

    init {
        programId = glCreateProgram()

        stages = glslProgram.sourceCode.map { (stage, code) ->

            val stageShaderId = glCreateShader(stage.glId)
            glShaderSource(stageShaderId, code)
            glCompileShader(stageShaderId)

            val compileStatus = glGetShaderi(stageShaderId, GL_COMPILE_STATUS)
            if(compileStatus != GL_TRUE) {
                val errorLog = glGetShaderInfoLog(stageShaderId, 16384)

                logger.error("Shader compilation error for stage $stage: of shader ${glslProgram.name}")
                logger.error(errorLog)
            }

            glAttachShader(programId, stageShaderId)

            stageShaderId
        }

        glLinkProgram(programId)

        val linkStatus = glGetProgrami(programId, GL_LINK_STATUS)
        if(linkStatus != GL_TRUE) {
            val errorLog = glGetProgramInfoLog(programId, 16384)

            logger.error("Shader link error on shader ${glslProgram.name}")
            logger.error(errorLog)
        }

        uboIndexes = glslProgram.resources.filterIsInstance<GLSLUniformBlock>().map {
            Pair(it, glGetUniformBlockIndex(programId, it.rawName))
        }.toMap()

        var uboBindingslot = 0
        uboBindings = glslProgram.resources.filterIsInstance<GLSLUniformBlock>().map {
            println(""+uboIndexes[it]!!+"wro"+uboBindingslot)
            glUniformBlockBinding(programId, uboIndexes[it]!!, uboBindingslot)
            Pair(it, uboBindingslot++)
        }.toMap()
    }

    val ShaderStage.glId: Int
        get() = when (this) {
            ShaderStage.VERTEX -> GL_VERTEX_SHADER
            ShaderStage.GEOMETRY -> GL_GEOMETRY_SHADER
            ShaderStage.FRAGMENT -> GL_FRAGMENT_SHADER
        }

    override fun cleanup() {
        glDeleteProgram(programId)
        stages.forEach {
            glDeleteShader(it)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.gfx_gl.shader")
    }
}