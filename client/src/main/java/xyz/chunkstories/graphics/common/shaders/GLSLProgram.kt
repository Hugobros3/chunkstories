package xyz.chunkstories.graphics.common.shaders

import xyz.chunkstories.api.graphics.shader.ShaderStage

data class GLSLProgram(
        val name: String,
        val dialect: GLSLDialect,
        val vertexInputs: List<GLSLVertexInput>,
        val fragmentOutputs: List<GLSLFragmentOutput>,
        val instancedInputs: List<GLSLInstancedInput>,
        val resources: List<GLSLResource>,
        val materialImages: List<MaterialImage>,
        val sourceCode: Map<ShaderStage, String>
)

data class GLSLVertexInput(val name: String, val format: GLSLType.BaseType, val location: Int)

data class GLSLFragmentOutput(val name: String, val format: GLSLType.BaseType, val location: Int)

data class GLSLInstancedInput(val name: String, val struct: GLSLType.JvmStruct, val shaderStorage: GLSLShaderStorage)

/** Represents one image that varies per material */
data class MaterialImage(val name: String)

enum class GLSLDialect {
    VULKAN,
    OPENGL4,
}

/** Describes anything that can be bound to a shader, has a name, a descriptor set slot and a binding within that slot. */
interface GLSLResource {
    val name: String
    val descriptorSetSlot: Int
    val binding: Int
}

data class GLSLUniformBlock(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int,
        val struct: GLSLType.JvmStruct) : GLSLResource

data class GLSLShaderStorage(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int
) : GLSLResource

/** Represents all kind of combined sampler/image resources */
interface GLSLUniformSampledImage : GLSLResource {

    /** For convenience every sampled image is mapped at compile-time to some texture unit.
     * Only used when rendering with OpenGL of course. */
    val openglTextureUnit: Int
}

/** Represents a (potentially an array of) sampler2D uniform resource declared in one of the stages of the GLSL program */
data class GLSLUniformSampledImage2D(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int,
        override val openglTextureUnit: Int,
        val count: Int) : GLSLUniformSampledImage

data class GLSLUniformSampledImage3D(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int,
        override val openglTextureUnit: Int,
        val count: Int) : GLSLUniformSampledImage

/** The onion sort of 2D textures */
data class GLSLUniformSampledImage2DArray(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val openglTextureUnit: Int,
        override val binding: Int) : GLSLUniformSampledImage

/** Standalone image */
data class GLSLUniformImage2D(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int,
        val count: Int) : GLSLResource

data class GLSLUniformSampler(
        override val name: String,
        override val descriptorSetSlot: Int,
        override val binding: Int
) : GLSLResource