package xyz.chunkstories.graphics.vulkan.shaders

import de.unisaarland.zhady.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAddress
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.GLSLGraphicsProgram
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import java.io.File
import java.nio.ByteBuffer

fun supportsSpirv13(backend: VulkanGraphicsBackend): Boolean {
    if(backend.vulkanVersion.major > 1)
        return true
    if(backend.vulkanVersion.minor >= 1)
        return true
    return false
}

data class VulkanShaderProgram internal constructor(val backend: VulkanGraphicsBackend, val basePath: String, val glslProgram: GLSLGraphicsProgram) : Cleanable {
    val spirvCode = generateSpirV(glslProgram, supportsSpirv13(backend))
    val modules: Map<ShaderStage, ShaderModule>

    private val maxSlotUsed: Int

    val slotLayouts: Array<DescriptorSlotLayout>

    data class CompiledSpvGraphicsPipeline(val stages: Map<ShaderStage, ByteBuffer>)

    fun generateSpirV(program: GLSLGraphicsProgram, spv13: Boolean): CompiledSpvGraphicsPipeline {
        return CompiledSpvGraphicsPipeline (program.sourceCode.mapValues {
            val (stage, module) = it;
            val spirvSize = stackCallocPointer(1);
            val spirvPtr = stackCallocPointer(1);
            val spvBackendConfig = shady.shd_default_spirv_backend_config()
            shady.shd_emit_spirv(backend.shaderFactory.driverConfig.config, spvBackendConfig, SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false), SWIGTYPE_p_size_t(memAddress(spirvSize), false), SWIGTYPE_p_p_char(memAddress(spirvPtr), false))
            MemoryUtil.memByteBuffer(spirvPtr.get(), spirvSize.get().toInt())
        })
    }

    init {
        stackPush()

        if (backend.enableValidation) {
            for ((stage, txt) in glslProgram.sourceCode) {
                val dumpFile = File("cache/debug/shaders/${basePath}_$stage.glsl")
                dumpFile.parentFile.mkdirs()
                dumpFile.delete()

                // TODO("implement shady dump here")
                // dumpFile.writeText(txt)
            }
        }

        modules = spirvCode.stages.mapValues { ShaderModule(backend, it.value) }

        maxSlotUsed = glslProgram.resources.maxByOrNull { it.locator.descriptorSetSlot }?.locator?.descriptorSetSlot ?: -1

        slotLayouts = Array(maxSlotUsed + 1) { slot ->
            val slotResources = glslProgram.resources.filter { it.locator.descriptorSetSlot == slot }.toSet()
            backend.descriptorMegapool.acquireDescriptorSlotLayout(slotResources)
        }

        stackPop()
    }

    override fun cleanup() {
        modules.values.forEach { it.cleanup() }
        slotLayouts.forEach { backend.descriptorMegapool.releaseDescriptorSlotLayout(it) }
    }
}