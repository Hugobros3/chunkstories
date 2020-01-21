package xyz.chunkstories.graphics.vulkan.shaders

import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.preprocessing.updateFrequency
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.ResourceLocationAssigner
import xyz.chunkstories.graphics.vulkan.devices.LogicalDevice
import xyz.chunkstories.graphics.vulkan.textures.GlobalTextures

class VulkanResourceLocationAssigner(logicalDevice: LogicalDevice) : ResourceLocationAssigner {
    val maxSets = logicalDevice.physicalDevice.maxBoundSets.coerceIn(1..8)
    val nextFreeBinding = IntArray(maxSets)

    private fun UniformUpdateFrequency.toSet() = if(maxSets >= 8) when(this) {
        UniformUpdateFrequency.ONCE_PER_BATCH -> 5
        UniformUpdateFrequency.ONCE_PER_SYSTEM -> 4
        UniformUpdateFrequency.ONCE_PER_RENDER_TASK -> 3
        UniformUpdateFrequency.ONCE_PER_FRAME -> 2
    } else {
        // swiftshader compatibility
        2
    }

    private val MAGIC_TEXTURING_SET = 0
    private val GENERAL_TEXTURES_SET = 1

    override fun assignSSBO(name: String, instanced: Boolean): ResourceLocator {
        return if (instanced) {
            val set = UniformUpdateFrequency.ONCE_PER_BATCH.toSet()
            ResourceLocator(set, nextFreeBinding[set]++)
        } else {
            val set = UniformUpdateFrequency.ONCE_PER_BATCH.toSet()
            ResourceLocator(set, nextFreeBinding[set]++)
        }
    }

    override fun assignInlinedUBO(jvmStruct: GLSLType.JvmStruct): ResourceLocator {
        val set = jvmStruct.kClass.updateFrequency().toSet()
        return ResourceLocator(set, nextFreeBinding[set]++)
    }

    override fun assignSampler(): ResourceLocator {
        val set = MAGIC_TEXTURING_SET
        return ResourceLocator(set, nextFreeBinding[set]++)
    }

    override fun assignSeperateImage(separateImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator {
        val set = when (separateImageName) {
            in GlobalTextures.magicTexturesNames -> MAGIC_TEXTURING_SET
            in materialBoundResources -> UniformUpdateFrequency.ONCE_PER_BATCH.toSet()
            else -> GENERAL_TEXTURES_SET
        }
        return ResourceLocator(set, nextFreeBinding[set]++)
    }

    override fun assignSampledImage(sampledImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator {
        val set = when (sampledImageName) {
            in GlobalTextures.magicTexturesNames -> MAGIC_TEXTURING_SET
            in materialBoundResources -> UniformUpdateFrequency.ONCE_PER_BATCH.toSet()
            else -> GENERAL_TEXTURES_SET
        }
        return ResourceLocator(set, nextFreeBinding[set]++)
    }

}