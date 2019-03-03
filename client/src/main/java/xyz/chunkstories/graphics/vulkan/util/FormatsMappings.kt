package xyz.chunkstories.graphics.vulkan.util

import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.graphics.vulkan.util.VulkanFormat.*

fun getVulkanFormat(vertexFormat: VertexFormat, components: Int) =
        //TODO these might not be available everywhere!
    when(components) {
        1 -> when(vertexFormat) {
            VertexFormat.FLOAT -> VK_FORMAT_R32_SFLOAT
            VertexFormat.HALF_FLOAT -> VK_FORMAT_R16_SFLOAT
            VertexFormat.INTEGER -> VK_FORMAT_R32_SINT
            VertexFormat.SHORT -> TODO()
            VertexFormat.USHORT -> TODO()
            VertexFormat.NORMALIZED_USHORT -> TODO()
            VertexFormat.BYTE -> TODO()
            VertexFormat.UBYTE -> TODO()
            VertexFormat.NORMALIZED_UBYTE -> TODO()
            VertexFormat.U1010102 -> TODO()
        }
        2 -> when(vertexFormat) {
            VertexFormat.FLOAT -> VK_FORMAT_R32G32_SFLOAT
            VertexFormat.HALF_FLOAT -> TODO()
            VertexFormat.INTEGER -> TODO()
            VertexFormat.SHORT -> TODO()
            VertexFormat.USHORT -> TODO()
            VertexFormat.NORMALIZED_USHORT -> TODO()
            VertexFormat.BYTE -> TODO()
            VertexFormat.UBYTE -> TODO()
            VertexFormat.NORMALIZED_UBYTE -> TODO()
            VertexFormat.U1010102 -> TODO()
        }
        3 -> when(vertexFormat) {
            VertexFormat.FLOAT -> VK_FORMAT_R32G32B32_SFLOAT
            VertexFormat.HALF_FLOAT -> TODO()
            VertexFormat.INTEGER -> TODO()
            VertexFormat.SHORT -> TODO()
            VertexFormat.USHORT -> TODO()
            VertexFormat.NORMALIZED_USHORT -> TODO()
            VertexFormat.BYTE -> TODO()
            VertexFormat.UBYTE -> TODO()
            VertexFormat.NORMALIZED_UBYTE -> TODO()
            VertexFormat.U1010102 -> TODO()
        }
        4 -> when(vertexFormat) {
            VertexFormat.FLOAT -> VK_FORMAT_R32G32B32A32_SFLOAT
            VertexFormat.HALF_FLOAT -> TODO()
            VertexFormat.INTEGER -> TODO()
            VertexFormat.SHORT -> TODO()
            VertexFormat.USHORT -> TODO()
            VertexFormat.NORMALIZED_USHORT -> TODO()
            VertexFormat.BYTE -> VK_FORMAT_R8G8B8A8_SINT
            VertexFormat.UBYTE -> VK_FORMAT_R8G8B8A8_UINT
            VertexFormat.NORMALIZED_UBYTE -> VK_FORMAT_R8G8B8A8_UNORM
            VertexFormat.U1010102 -> TODO()
        }
        else -> throw Exception()
    }
