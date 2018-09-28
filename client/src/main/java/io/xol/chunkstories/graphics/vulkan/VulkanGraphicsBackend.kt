package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class VulkanGraphicsBackend(window: GLFWWindow) : GLFWBasedGraphicsBackend(window) {
    internal val enableValidation = true

    val requiredDeviceExtensions = listOf(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)

    private var instance: VkInstance
    private val debugCallback: Long

    /** All the physical devices available to Vulkan */
    val physicalDevices: List<PhysicalDevice>

    /** The physical device in use by the application */
    val physicalDevice: PhysicalDevice

    /** The logical device in use by the application, derived from the physicalDevice */
    val logicalDevice: LogicalDevice

    /** The actual surface we're drawing onto */
    internal var surface: WindowSurface
    internal var swapchain: SwapChain

    val renderToBackbuffer : VulkanRenderPass

    val imageAvailableSemaphore : VkSemaphore
    val renderFinishedSemaphore : VkSemaphore

    val triangleDrawer : TriangleDrawer

    init {
        if (!glfwVulkanSupported())
            throw Exception("Vulkan is not supported on this machine")

        val requiredExtensions = glfwGetRequiredInstanceExtensions() ?: throw Exception("Vulkan is not supported for windowed rendering on this machine.")

        instance = createVkInstance(requiredExtensions)
        debugCallback = setupDebug(instance)
        surface = WindowSurface(instance, window)

        physicalDevices = enumeratePhysicalDevices()
        physicalDevice = pickPhysicalDevice(true)

        logicalDevice = LogicalDevice(this, physicalDevice)

        renderToBackbuffer = VulkanRenderPass(this)
        swapchain = SwapChain(this, renderToBackbuffer)

        imageAvailableSemaphore = createSemaphore()
        renderFinishedSemaphore = createSemaphore()

        triangleDrawer = TriangleDrawer(this)
    }

    override fun drawFrame(frameNumber: Int) {
        stackPush()
        val pImageIndex = stackMallocInt(1)
        vkAcquireNextImageKHR(logicalDevice.vkDevice, swapchain.handle, Long.MAX_VALUE, imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex)
        val imageIndex = pImageIndex.get(0)

        triangleDrawer.drawTriangle(imageIndex)

        val presentInfo = VkPresentInfoKHR.callocStack().sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR).apply {
            val waitSemaphores = stackMallocLong(1)
            waitSemaphores.put(0, renderFinishedSemaphore)
            pWaitSemaphores(waitSemaphores)

            val swapChains = stackMallocLong(1)
            swapChains.put(0, swapchain.handle)
            pSwapchains(swapChains)
            swapchainCount(1)

            pImageIndices(pImageIndex)
            pResults(null)
        }

        vkQueuePresentKHR(logicalDevice.presentationQueue.handle, presentInfo)

        stackPop()
    }

    override fun captureFramebuffer(): BufferedImage {
        throw UnsupportedOperationException("Not yet.")
    }

    /** Creates a Vulkan instance */
    private fun createVkInstance(requiredExtensions: PointerBuffer): VkInstance = stackPush().use {
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
            ppEnabledLayerNames(requestedLayers)
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
                Thread.dumpStack()
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

    private fun enumeratePhysicalDevices(): List<PhysicalDevice> = stackPush().use {
        logger.debug("Enumerating physical devices...")
        val pPhysicalDeviceCount = it.mallocInt(1)
        vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null).ensureIs("Failed to count physical devices !", VK_SUCCESS)

        // Then make a suitably sized array and grab'em
        val pPhysicalDevices = it.mallocPointer(pPhysicalDeviceCount.get(0))
        vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices).ensureIs("Failed to enumerate physical devices !", VK_SUCCESS, VK_INCOMPLETE)

        val physicalDevices = mutableListOf<PhysicalDevice>()
        for (physicalDeviceHandle in pPhysicalDevices) {
            val physicalDevice = PhysicalDevice(this, VkPhysicalDevice(physicalDeviceHandle, instance))

            if (physicalDevice.suitable)
                physicalDevices.add(physicalDevice)
            else
                logger.debug("Ignoring unsuitable physical device : $physicalDevice")
        }

        return physicalDevices
    }

    private fun pickPhysicalDevice(prompt: Boolean): PhysicalDevice {
        logger.debug("Selecting physical device...")
        var bestPhysicalDevice: PhysicalDevice? = null

        val preferredDeviceId = window.client.configuration.getIntValue("client.graphics.vulkan.device")
        if (preferredDeviceId != 0) {
            bestPhysicalDevice = physicalDevices.find { it.deviceId == preferredDeviceId }
        } else if (prompt) {
            //TODO ask the user with a pop-up
        }

        // The user didn't pick his favourite device ? Use the most suitable one
        if (bestPhysicalDevice == null)
            bestPhysicalDevice = physicalDevices.maxBy(PhysicalDevice::fitnessScore)

        logger.debug("Picking physical device $bestPhysicalDevice")

        return bestPhysicalDevice ?: throw Exception("Could not find suitable physical device !")
    }

    override fun createDrawingSystem(clazz: Class<DrawingSystem>): DrawingSystem {
        when (clazz) {
            FarTerrainDrawer::class.java -> {
                TODO("you have to implement the common drawing systems in your graphicsBackend")
            }
        }

        TODO("not implemented")
    }

    override fun cleanup() {
        logicalDevice.cleanup()

        vkDestroyInstance(instance, null)
        logger.debug("Successfully finished cleaning up Vulkan objects")
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}