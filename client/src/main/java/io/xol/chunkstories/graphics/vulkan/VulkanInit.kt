package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkPhysicalDevice
import java.lang.Exception

/** Creates a Vulkan instance */
fun createVkInstance(requiredExtensions: PointerBuffer, enableValidation: Boolean): VkInstance = stackPush().use {
    val appInfoStruct = VkApplicationInfo.callocStack().sType(VK_STRUCTURE_TYPE_APPLICATION_INFO).apply {
        pApplicationName(stackUTF8("Chunk Stories"))
        pEngineName(stackUTF8("Chunk Stories Vulkan Backend"))

        //No clue which version to use, same one as the tutorial I guess
        apiVersion(VK_MAKE_VERSION(1, 0, 2))
    }

    val requestedExtensions = stackMallocPointer(requiredExtensions.remaining() + 1)
    requestedExtensions.put(requiredExtensions)
    requestedExtensions.put(stackUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
    requestedExtensions.flip()

    var requestedLayers: PointerBuffer? = null
    if (enableValidation) {
        requestedLayers = stackCallocPointer(1)
        requestedLayers.put(stackUTF8("VK_LAYER_LUNARG_standard_validation"))
        requestedLayers.flip()
    }

    val createInfoStruct = VkInstanceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO).apply {
        pApplicationInfo(appInfoStruct)
        ppEnabledExtensionNames(requestedExtensions)
        if (requestedLayers != null) ppEnabledLayerNames(requestedLayers)
    }

    val pInstance = stackMallocPointer(1)
    vkCreateInstance(createInfoStruct, null, pInstance).ensureIs(VK_SUCCESS, "Failed to create Vulkan instance")

    VkInstance(pInstance.get(0), createInfoStruct)
}

fun enumerateAndPickPhysicalDevice(prompt : Boolean) : VkPhysicalDevice = TODO()

//mek device

// mek gud

private fun Int.ensureIs(compareTo: Int, exceptionMessage: String) =
        if (this != compareTo) throw Exception(exceptionMessage) else Unit

