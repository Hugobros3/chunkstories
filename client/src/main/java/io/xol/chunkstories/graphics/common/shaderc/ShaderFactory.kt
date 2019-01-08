package io.xol.chunkstories.graphics.common.shaderc

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass

open class ShaderFactory(open val classLoader: ClassLoader) {

    enum class GLSLDialect {
        OPENGL4,
        VULKAN
    }

    /** All the data structure that are explicitely included in the shader string *or* implicitely included due to use in an included struct */
    val structures = mutableMapOf<KClass<InterfaceBlock>, InterfaceBlockGLSLMapping>()

    fun translateGLSL(dialect: GLSLDialect, stagesCode: Map<ShaderStage, String>, context: Content? = null, shadersAssetBaseDir : String? = null): GLSLProgram =
            SpirvCrossHelper.translateGLSLDialect(this, dialect, stagesCode, context, shadersAssetBaseDir)

    data class GLSLProgram(val sourceCode: Map<ShaderStage, String>, val vertexInputs: List<GLSLVertexAttribute>, val resources: List<GLSLUniformResource>)

    data class GLSLVertexAttribute(val name: String, val format: GLSLBaseType, val location: Int, val interfaceBlock: InterfaceBlockGLSLMapping?)

    interface GLSLUniformResource {
        val name: String
        val descriptorSetSlot: Int
        val binding: Int
    }

    data class GLSLUniformBlock(
            override val name: String,
            override val descriptorSetSlot: Int,
            override val binding: Int,
            val mapper: InterfaceBlockGLSLMapping) : GLSLUniformResource

    data class GLSLUnusedUniform(
            override val name: String,
            override val descriptorSetSlot: Int,
            override val binding: Int
    ) : GLSLUniformResource

    /** Represents a (potentially an array of) sampler2D uniform resource declared in one of the stages of the GLSL program */
    data class GLSLUniformSampler2D(
            override val name: String,
            override val descriptorSetSlot: Int,
            override val binding: Int,
            val count: Int) : GLSLUniformResource
}