package xyz.chunkstories.graphics.opengl.shaders

import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.ResourceLocator
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.ResourceLocationAssigner

class OpenglResourceLocationAssigner : ResourceLocationAssigner {
    var nextFreeBinding = 0

    override fun assignSSBO(name: String, instanced: Boolean) = ResourceLocator(descriptorSetSlot = 0, binding = nextFreeBinding++)
    override fun assignInlinedUBO(jvmStruct: GLSLType.JvmStruct) = ResourceLocator(descriptorSetSlot = 0, binding = nextFreeBinding++)
    override fun assignSampler(): ResourceLocator = ResourceLocator(descriptorSetSlot = 0, binding = nextFreeBinding++)
    override fun assignSeperateImage(separateImageName: String, materialBoundResources: MutableSet<String>) = ResourceLocator(descriptorSetSlot = 0, binding = nextFreeBinding++)
    override fun assignSampledImage(sampledImageName: String, materialBoundResources: MutableSet<String>) = ResourceLocator(descriptorSetSlot = 0, binding = nextFreeBinding++)
}