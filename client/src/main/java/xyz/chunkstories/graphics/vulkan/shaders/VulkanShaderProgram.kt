package xyz.chunkstories.graphics.vulkan.shaders

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.SpirvCrossHelper
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import java.io.File

fun supportsSpirv13(backend: VulkanGraphicsBackend): Boolean {
    if(backend.vulkanVersion.major > 1)
        return true
    if(backend.vulkanVersion.minor >= 1)
        return true
    return false
}

data class VulkanShaderProgram internal constructor(val backend: VulkanGraphicsBackend, val basePath: String, val glslProgram: GLSLProgram) : Cleanable {
    val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram, supportsSpirv13(backend))
    val modules: Map<ShaderStage, ShaderModule>

    private val maxSlotUsed: Int

    val slotLayouts: Array<DescriptorSlotLayout>

    init {
        stackPush()

        if (backend.enableValidation) {
            for ((stage, txt) in glslProgram.sourceCode) {
                val dumpFile = File("cache/debug/shaders/${basePath}_$stage.glsl")
                dumpFile.parentFile.mkdirs()
                dumpFile.delete()

                dumpFile.writeText(txt)
            }
        }

        modules = spirvCode.stages.mapValues { ShaderModule(backend, it.value) }

        maxSlotUsed = glslProgram.resources.maxBy { it.locator.descriptorSetSlot }?.locator?.descriptorSetSlot ?: -1

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