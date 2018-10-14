package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.devices.LogicalDevice
import io.xol.chunkstories.graphics.vulkan.devices.PhysicalDevice
import io.xol.chunkstories.graphics.vulkan.resources.VulkanMemoryManager
import io.xol.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import io.xol.chunkstories.graphics.vulkan.swapchain.SwapChain
import io.xol.chunkstories.graphics.vulkan.swapchain.WindowSurface
import io.xol.chunkstories.graphics.vulkan.systems.VulkanGuiPass
import io.xol.chunkstories.graphics.vulkan.textures.VulkanTextures
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

import io.xol.chunkstories.graphics.vulkan.util.iterator

class VulkanGraphicsBackend(window: GLFWWindow) : GLFWBasedGraphicsBackend(window) {
    internal val enableValidation = true

    val requiredDeviceExtensions = listOf(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)

    private var instance: VkInstance
    private val debugCallback: Long
    private var cookie = true

    /** All the physical devices available to Vulkan */
    val physicalDevices: List<PhysicalDevice>

    /** The physical device in use by the application */
    val physicalDevice: PhysicalDevice

    /** The logical device in use by the application, derived from the physicalDevice */
    val logicalDevice: LogicalDevice

    val memoryManager: VulkanMemoryManager

    /** The actual surface we're drawing onto */
    internal var surface: WindowSurface
    internal var swapchain: SwapChain
        internal set

    val renderToBackbuffer : VulkanRenderPass

    val shaderFactory = VulkanShaderFactory(window.client)
    val textures: VulkanTextures

    var triangleDrawer : VulkanGuiPass

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

        memoryManager = VulkanMemoryManager(this, logicalDevice)
        textures = VulkanTextures(this)

        renderToBackbuffer = VulkanRenderPass(this)
        swapchain = SwapChain(this, renderToBackbuffer, null)

        GLFW.glfwSetWindowSizeCallback(window.glfwWindowHandle) { handle, newWidth, newHeight ->
                if(newWidth != 0 && newHeight != 0) {
                    window.width = newWidth
                    window.height = newHeight

                this@VulkanGraphicsBackend.swapchain.expired = true
            }
        }

        triangleDrawer = VulkanGuiPass(this, window.client.gui)
    }

    override fun drawFrame(frameNumber: Int) {
        val frame = swapchain.beginFrame(frameNumber)

        triangleDrawer.render(frame)

        swapchain.finishFrame(frame)
    }

    override fun captureFramebuffer(): BufferedImage {
        throw UnsupportedOperationException("Not yet.")
    }

    fun recreateSwapchainDependencies() {
        //triangleDrawer.cleanup()
        //triangleDrawer = VulkanGuiPass(this, window.client.gui)
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
        val jvmCallback = object : VkDebugReportCallbackEXT() {
            override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
                logger.error(VkDebugReportCallbackEXT.getString(pMessage))
                Thread.dumpStack()
                cookie = false
                return 0
            }
        }

        stackPush().use {
            val dbgSetupStruct = VkDebugReportCallbackCreateInfoEXT.callocStack().sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT).apply {
                pfnCallback(jvmCallback)
                flags(VK_DEBUG_REPORT_ERROR_BIT_EXT or VK_DEBUG_REPORT_WARNING_BIT_EXT)
            }

            val pCallback = MemoryStack.stackMallocLong(1)
            vkCreateDebugReportCallbackEXT(vkInstance, dbgSetupStruct, null, pCallback).ensureIs("Failed to create debug callback !", VK10.VK_SUCCESS)
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
        vkDeviceWaitIdle(logicalDevice.vkDevice)

        triangleDrawer.cleanup()

        renderToBackbuffer.cleanup()
        swapchain.cleanup()

        textures.cleanup()
        memoryManager.cleanup()

        logicalDevice.cleanup()

        vkDestroyDebugReportCallbackEXT(instance, debugCallback, null)
        if(cookie)
            logger.debug("You get a cookie for not making the validation layer unhappy :)")
        else
            logger.debug("The validation layer found errors, no cookie for you !")

        vkDestroyInstance(instance, null)
        logger.debug("Successfully finished cleaning up Vulkan objects")
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}