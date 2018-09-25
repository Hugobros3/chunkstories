package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage

class VulkanGraphicsBackend(window: GLFWWindow) : GLFWBasedGraphicsBackend(window) {
    private var instance: VkInstance
    private val debugCallback: Long

    private val physicalDevice: VkPhysicalDevice

    init {
        if(!glfwVulkanSupported())
            throw Exception("Vulkan is not supported on this machine")

        val requiredExtensions = glfwGetRequiredInstanceExtensions() ?: throw Exception("Vulkan is not supported for windowed rendering on this machine.")

        instance = createVkInstance(requiredExtensions, true)
        debugCallback = setupDebug(instance)

        physicalDevice = enumerateAndPickPhysicalDevice(true)
    }

    override fun drawFrame(frameNumber: Int) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun captureFramebuffer(): BufferedImage {
        throw UnsupportedOperationException("Not yet.")
    }

    /** Creates a Vulkan instance */
    private fun createVkInstance(requiredExtensions: PointerBuffer, enableValidation: Boolean): VkInstance = MemoryStack.stackPush().use {
        val appInfoStruct = VkApplicationInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO).apply {
            pApplicationName(MemoryStack.stackUTF8("Chunk Stories"))
            pEngineName(MemoryStack.stackUTF8("Chunk Stories Vulkan Backend"))

            //No clue which version to use, same one as the tutorial I guess
            apiVersion(VK10.VK_MAKE_VERSION(1, 0, 2))
        }

        val requestedExtensions = MemoryStack.stackMallocPointer(requiredExtensions.remaining() + 1)
        requestedExtensions.put(requiredExtensions)
        requestedExtensions.put(MemoryStack.stackUTF8(EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
        requestedExtensions.flip()

        var requestedLayers: PointerBuffer? = null
        if (enableValidation) {
            requestedLayers = MemoryStack.stackCallocPointer(1)
            requestedLayers.put(MemoryStack.stackUTF8("VK_LAYER_LUNARG_standard_validation"))
            requestedLayers.flip()
        }

        val createInfoStruct = VkInstanceCreateInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO).apply {
            pApplicationInfo(appInfoStruct)
            ppEnabledExtensionNames(requestedExtensions)
            if (requestedLayers != null) ppEnabledLayerNames(requestedLayers)
        }

        val pInstance = MemoryStack.stackMallocPointer(1)
        VK10.vkCreateInstance(createInfoStruct, null, pInstance).ensureIs("Failed to create Vulkan instance", VK10.VK_SUCCESS)

        val vkInstance = VkInstance(pInstance.get(0), createInfoStruct)

        logger.info("Successfully created Vulkan instance")

        return vkInstance
    }

    private fun setupDebug(vkInstance: VkInstance): Long {
        val callback = object : VkDebugReportCallbackEXT() {
            override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
                logger.error(VkDebugReportCallbackEXT.getString(pMessage))
                return 0
            }
        }

        MemoryStack.stackPush().use {
            val dbgSetupStruct = VkDebugReportCallbackCreateInfoEXT.callocStack().sType(EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT).apply {
                pfnCallback(callback)
                flags(EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT or EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT)
            }

            val pCallback = MemoryStack.stackMallocLong(1)
            EXTDebugReport.vkCreateDebugReportCallbackEXT(vkInstance, dbgSetupStruct, null, pCallback).ensureIs("Failed to create debug callback !", VK10.VK_SUCCESS)
            logger.info("Successfully registered debug callback")
            return pCallback.get(0)
        }
    }

    fun enumerateAndPickPhysicalDevice(prompt: Boolean): VkPhysicalDevice = MemoryStack.stackPush().use {
        logger.debug("Picking device...")
        val pPhysicalDeviceCount = it.mallocInt(1)
        vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null).ensureIs("Failed to count physical devices !", VK_SUCCESS, VK_INCOMPLETE)

        // Then make a suitably sized array and grab'em
        val pPhysicalDevices = it.mallocPointer(pPhysicalDeviceCount.get(0))
        vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices).ensureIs("Failed to enumerate physical devices !", VK_SUCCESS, VK_INCOMPLETE)

        //We are only interested in the first device
        val physicalDeviceHandle = pPhysicalDevices.get(0)

        val physicalDevice = VkPhysicalDevice(physicalDeviceHandle, instance)

        val pProperties = VkPhysicalDeviceProperties.callocStack(it)
        VK10.vkGetPhysicalDeviceProperties(physicalDevice, pProperties)

        logger.debug("Vulkan device: ${pProperties.deviceNameString()} (${pProperties.deviceType().physicalDeviceTypeName()}) ")
        logger.debug("${pProperties.limits().bufferImageGranularity()}")

        return physicalDevice
    }

    override fun createDrawingSystem(clazz: Class<DrawingSystem>): DrawingSystem {
        when(clazz) {
            FarTerrainDrawer::class.java -> {
                TODO("you have to implement the common drawing systems in your backend")
            }
        }

        TODO("not implemented")
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}