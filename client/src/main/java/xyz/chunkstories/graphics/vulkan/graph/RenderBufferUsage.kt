package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.vulkan.util.VkImageLayout

enum class UsageType {
    NONE,
    INPUT,
    OUTPUT
}

enum class AttachementType {
    COLOR, DEPTH
}

fun getLayoutForStateAndType(usageType: UsageType, attachementType: AttachementType) : VkImageLayout = when(usageType) {
    UsageType.NONE -> VK_IMAGE_LAYOUT_UNDEFINED
    UsageType.INPUT -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
    UsageType.OUTPUT -> when(attachementType) {
        AttachementType.COLOR -> VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
        AttachementType.DEPTH -> VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL
    }
}

fun AttachementType.aspectMask() : Int = when(this) {
    AttachementType.COLOR -> VK_IMAGE_ASPECT_COLOR_BIT
    AttachementType.DEPTH -> VK_IMAGE_ASPECT_DEPTH_BIT
}

fun UsageType.accessMask() : Int = when(this) {
    UsageType.NONE -> /** well it's unused duh */ 0
    UsageType.INPUT -> VK_ACCESS_SHADER_READ_BIT

    //TODO if no blend we might not even need the read thing
    UsageType.OUTPUT -> VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
}

//data class RenderBufferUsage(val usageType: UsageType, val attachementType: AttachementType)