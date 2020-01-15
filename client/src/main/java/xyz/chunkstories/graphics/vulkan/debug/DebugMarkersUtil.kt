package xyz.chunkstories.graphics.vulkan.debug

import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.EXTDebugMarker
import org.lwjgl.vulkan.EXTDebugMarker.VK_STRUCTURE_TYPE_DEBUG_MARKER_MARKER_INFO_EXT
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDebugMarkerMarkerInfoEXT
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskInstance
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import java.security.MessageDigest

class DebugMarkersUtil(val backend: VulkanGraphicsBackend) {
    val md = MessageDigest.getInstance("MD5")

    fun enterRenderTask(commandBuffer: VkCommandBuffer, renderTask: RenderTaskInstance) {
        stackPush()
        val markerInfo = VkDebugMarkerMarkerInfoEXT.calloc()
        markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_MARKER_MARKER_INFO_EXT)
        val prettyName = renderTask.name()
        val color = color(prettyName)
        markerInfo.color().apply { put(0, color.x) ; put(1, color.y) ; put(2, color.z) ; put(3, 1f) }
        markerInfo.pMarkerName(stackUTF8(prettyName))
        EXTDebugMarker.vkCmdDebugMarkerBeginEXT(commandBuffer, markerInfo)
        stackPop()
    }

    fun leaveRenderTask(commandBuffer: VkCommandBuffer) {
        stackPush()
        EXTDebugMarker.vkCmdDebugMarkerEndEXT(commandBuffer)
        stackPop()
    }

    fun enterPass(commandBuffer: VkCommandBuffer, pass: PassInstance) {
        stackPush()
        val markerInfo = VkDebugMarkerMarkerInfoEXT.callocStack()
        markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_MARKER_MARKER_INFO_EXT)
        val prettyName = pass.name()
        val color = color(prettyName).mul(0.4f)
        color.add(0.1f, 0.1f, 0.1f)
        markerInfo.color().apply { put(0, color.x) ; put(1, color.y) ; put(2, color.z) ; put(3, 1f) }
        markerInfo.pMarkerName(stackUTF8(prettyName))
        EXTDebugMarker.vkCmdDebugMarkerBeginEXT(commandBuffer, markerInfo)
        stackPop()
    }

    fun leavePass(commandBuffer: VkCommandBuffer) {
        stackPush()
        EXTDebugMarker.vkCmdDebugMarkerEndEXT(commandBuffer)
        stackPop()
    }

    fun enterSystem(commandBuffer: VkCommandBuffer, pass: PassInstance, systemName: String) {
        stackPush()
        val markerInfo = VkDebugMarkerMarkerInfoEXT.callocStack()
        markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_MARKER_MARKER_INFO_EXT)
        val prettyName = pass.name()+"::"+systemName
        val color = color(prettyName).mul(0.5f)
        color.add(0.5f, 0.5f, 0.5f)
        markerInfo.color().apply { put(0, color.x) ; put(1, color.y) ; put(2, color.z) ; put(3, 1f) }
        markerInfo.pMarkerName(stackUTF8(prettyName))
        EXTDebugMarker.vkCmdDebugMarkerBeginEXT(commandBuffer, markerInfo)
        stackPop()
    }

    fun leaveSystem(commandBuffer: VkCommandBuffer) {
        stackPush()
        EXTDebugMarker.vkCmdDebugMarkerEndEXT(commandBuffer)
        stackPop()
    }

    fun color(txt: String): Vector3f {
        val digested = md.digest(txt.toByteArray())
        val r = (digested[0].toInt() and 0xFF).toFloat() / 255f
        val g = (digested[1].toInt() and 0xFF).toFloat() / 255f
        val b = (digested[2].toInt() and 0xFF).toFloat() / 255f

        return Vector3f(r, g, b)
    }
}

private fun PassInstance.name(): String {
    return this.taskInstance.name()+"::"+this.declaration.name
}

private fun RenderTaskInstance.name(): String {
    val parent = this.requester

    val prefix = if(parent != null)
        parent.name()+"::"
    else
        ""

    return this.declaration.name+"("+this.name+")"
}
