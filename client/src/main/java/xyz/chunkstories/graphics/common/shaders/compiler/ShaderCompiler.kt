package xyz.chunkstories.graphics.common.shaders.compiler

import graphics.scenery.spirvcrossj.Loader
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.compiler.postprocessing.annotateForNonUniformAccess
import xyz.chunkstories.graphics.common.shaders.compiler.preprocessing.*
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.addDecorations
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.buildIntermediaryStructure
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.createShaderResources
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.toIntermediateGLSL
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import kotlin.reflect.KClass

abstract class ShaderCompiler(val dialect: GLSLDialect) {

    abstract val classLoader: ClassLoader
    abstract val content: Content?

    /** We keep track of all the JVM classes we encounter so we don't have to do the mapping into GLSL structs every time */
    val jvmGlslMappings = mutableMapOf<KClass<InterfaceBlock>, GLSLType.JvmStruct>()

    init {
        Loader.loadNatives()
        //initSpirvCross()
    }

    fun loadGLSLProgram(shaderName: String) : GLSLProgram {
        val shaderBaseDir = "shaders/$shaderName"

        val vertexShader = readShaderFile("$shaderBaseDir/$shaderName.vert") ?: throw Exception("Vertex shader stage not found in either built-in resources or assets for shader: $shaderName")
        val fragmentShader = readShaderFile("$shaderBaseDir/$shaderName.frag") ?: throw Exception("Fragment shader stage not found in either built-in resources or assets for shader: $shaderName")
        //TODO add optional geometry/tesselation stages here

        var stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        //stages = stages.mapValues { (stage, shaderCode) -> removeVersionString(shaderCode) }

        // Process #include statements
        stages = stages.mapValues { (stage, shaderCode) -> processFileIncludes(shaderBaseDir, shaderCode) }

        // Process #using struct statements
        // Find them and extract the required classes
        val jvmClassesUsed = stages.mapValues { (_, shaderCode) -> findUsedJvmClasses(shaderCode) }
        // Obtain/translate these class into the correct GLSL structs
        val jvmStructsUsed = jvmClassesUsed.mapValues { (_, it) -> it.map { getGlslStruct(it) } }
        // Add their declarations, in the right order.
        stages = stages.mapValues { (stage, shaderCode) -> addStructsDeclaration(shaderCode, jvmStructsUsed[stage]!! ) }

        stages = stages.mapValues { (stage, shaderCode) -> inlineUniformStructs(shaderCode, jvmStructsUsed[stage]!! ) }
        //TODO virtual texturing magic code
        //TODO per-instance data magic code

        val vertexInputs = analyseVertexShaderInputs(stages[ShaderStage.VERTEX]!!)
        val fragmentOutputs = analyseFragmentShaderOutputs(stages[ShaderStage.FRAGMENT]!!)

        val intermediaryCompilationResults = buildIntermediaryStructure(stages)
        val resources = createShaderResources(intermediaryCompilationResults)

        addDecorations(intermediaryCompilationResults, resources)
        stages = toIntermediateGLSL(intermediaryCompilationResults)

        if(this is VulkanShaderFactory && this.backend.enableDivergingUniformSamplerIndexing)
            stages = stages.mapValues { (stage, shaderCode) -> annotateForNonUniformAccess(shaderCode) }

        return GLSLProgram(shaderName, dialect, vertexInputs, fragmentOutputs, resources, stages)
    }

    fun readShaderFile(path: String): String? {
        val internalResource = javaClass.getResource("/$path")
        if (internalResource != null) {
            return internalResource.readText()
        }

        val asset = content?.getAsset(path)
        if(asset != null) {
            return asset.reader().readText()
        }

        return null
    }

    companion object {
        val logger = LoggerFactory.getLogger("shaderCompiler")
    }
}