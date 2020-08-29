package xyz.chunkstories.graphics.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugMarker.VK_EXT_DEBUG_MARKER_EXTENSION_NAME
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.debug.DebugMarkersUtil
import xyz.chunkstories.graphics.vulkan.devices.LogicalDevice
import xyz.chunkstories.graphics.vulkan.devices.PhysicalDevice
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderGraph
import xyz.chunkstories.graphics.vulkan.memory.VulkanMemoryManager
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.resources.frameallocator.FrameDataAllocatorProvider
import xyz.chunkstories.graphics.vulkan.resources.frameallocator.createFrameDataAllocatorProvider
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import xyz.chunkstories.graphics.vulkan.swapchain.SwapChain
import xyz.chunkstories.graphics.vulkan.swapchain.WindowSurface
import xyz.chunkstories.graphics.vulkan.textures.VulkanTextures
import xyz.chunkstories.graphics.vulkan.textures.voxels.VulkanVoxelTexturesArray
import xyz.chunkstories.graphics.vulkan.util.RenderPassHelpers
import xyz.chunkstories.graphics.vulkan.util.VkRenderPass
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import xyz.chunkstories.graphics.vulkan.util.iterator
import xyz.chunkstories.graphics.vulkan.world.VulkanWorldRenderer
import xyz.chunkstories.voxel.VoxelTexturesSupport
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage

class VulkanGraphicsBackend(graphicsEngine: GraphicsEngineImplementation, window: GLFWWindow) : GLFWBasedGraphicsBackend(graphicsEngine, window), VoxelTexturesSupport {
    internal val enableValidation = window.client.arguments["enableValidation"] == "true"

    private var instance: VkInstance

    private var debugOutputEnable = false
    private var debugCallback: Long = 0L
    private var cookie = true

    private var debugMarkerEnable = false
    val debugMarketUtil: DebugMarkersUtil?

    /** All the physical devices available to Vulkan */
    private val physicalDevices: List<PhysicalDevice>

    data class VulkanVersion(val major: Int, val minor: Int, val rev: Int)
    lateinit var vulkanVersion: VulkanVersion
        private set

    /** The physical device in use by the application */
    val physicalDevice: PhysicalDevice

    /** The logical device in use by the application, derived from the physicalDevice */
    val logicalDevice: LogicalDevice

    val memoryManager: VulkanMemoryManager
    val frameDataAllocatorProvider: FrameDataAllocatorProvider

    /** The actual surface we're drawing onto */
    internal var surface: WindowSurface
    internal var swapchain: SwapChain

    val renderToBackbuffer : VkRenderPass

    val descriptorMegapool = DescriptorSetsMegapool(this)
    val shaderFactory: VulkanShaderFactory
    val textures: VulkanTextures

    var renderGraph: VulkanRenderGraph

    init {
        if (!glfwVulkanSupported())
            throw Exception("Vulkan is not supported on this machine")

        window.client.configuration.addOptions(VulkanBackendOptions.create(this))

        val glfwRequiredExtensions = glfwGetRequiredInstanceExtensions() ?: throw Exception("Vulkan is not supported for windowed rendering on this machine.")

        instance = createVkInstance(glfwRequiredExtensions)
        if(debugOutputEnable)
            debugCallback = setupDebug(instance)

        surface = WindowSurface(instance, window)

        physicalDevices = enumeratePhysicalDevices()
        physicalDevice = pickPhysicalDevice(true)

        if(physicalDevice.availableExtensions.contains(VK_EXT_DEBUG_MARKER_EXTENSION_NAME))
            debugMarkerEnable = true

        logicalDevice = LogicalDevice(this, physicalDevice)

        debugMarketUtil = if(debugMarkerEnable) DebugMarkersUtil(this) else null

        shaderFactory = VulkanShaderFactory(this, window.client, logicalDevice)
        memoryManager = VulkanMemoryManager(this, logicalDevice)
        frameDataAllocatorProvider = createFrameDataAllocatorProvider()

        textures = VulkanTextures(this)
        //virtualTexturing = VirtualTexturing(this)

        renderToBackbuffer = RenderPassHelpers.createWindowSurfaceRenderPass(this)
        swapchain = SwapChain(this, renderToBackbuffer, null)

        GLFW.glfwSetWindowSizeCallback(window.glfwWindowHandle) { handle, newWidth, newHeight ->
            println("resized 2 to $newWidth:$newHeight")

            if(newWidth != 0 && newHeight != 0) {
                window.width = newWidth
                window.height = newHeight

                this@VulkanGraphicsBackend.swapchain.expired = true
            }
        }

        GLFW.glfwSetFramebufferSizeCallback(window.glfwWindowHandle) { handle, newWidth, newHeight ->
            println("resized to $newWidth:$newHeight")

            if(newWidth != 0 && newHeight != 0) {
                window.width = newWidth
                window.height = newHeight

                this@VulkanGraphicsBackend.swapchain.expired = true
            }
        }

        GLFW.glfwSetWindowContentScaleCallback(window.glfwWindowHandle) { handle, xScale, yScale ->
            println("scaled to $xScale:$yScale")

            val w = intArrayOf(0)
            val h = intArrayOf(0)
            GLFW.glfwGetWindowSize(handle, w, h)

            window.width = (w[0] * xScale).toInt()
            window.height = (h[0] * yScale).toInt()

            this@VulkanGraphicsBackend.swapchain.expired = true
        }

        renderGraph = VulkanRenderGraph(this, queuedRenderGraph!!)
        queuedRenderGraph = null
    }

    override fun drawFrame(frameNumber: Int) {
        val queuedRenderGraph = this.queuedRenderGraph
        if(queuedRenderGraph != null) {
            vkDeviceWaitIdle(logicalDevice.vkDevice)

            swapchain.flush()

            renderGraph.cleanup()
            renderGraph = VulkanRenderGraph(this, queuedRenderGraph)
            this.queuedRenderGraph = null
        }

        val frame = swapchain.beginFrame(frameNumber)
        renderGraph.renderFrame(frame)
        swapchain.finishFrame(frame)
    }

    override fun captureFramebuffer(): BufferedImage {
        throw UnsupportedOperationException("Not yet.")
    }

    fun recreateSwapchainDependencies() {
        renderGraph.resizeBuffers()
    }

    /** Creates a Vulkan instance */
    private fun createVkInstance(requiredExtensions: PointerBuffer): VkInstance = stackPush().use {
        // List available Vulkan extensions
        val extensionsCount = stackInts(0)
        vkEnumerateInstanceExtensionProperties(null as CharSequence?, extensionsCount, null as VkExtensionProperties.Buffer?)
        val extensionProperties = VkExtensionProperties.calloc(extensionsCount[0])
        vkEnumerateInstanceExtensionProperties(null as CharSequence?, extensionsCount, extensionProperties)

        val availableInstanceExtensions = extensionProperties.map { it.extensionNameString() }
        logger.info("Available instance extensions: $availableInstanceExtensions")

        val missingInstanceExtensions = requiredInstanceExtensions.filter { !availableInstanceExtensions.contains(it) }
        if(missingInstanceExtensions.isNotEmpty()) {
            throw Exception("Missing some required instance extensions: $missingInstanceExtensions")
        }

        val instanceExtensionsToEnable = requiredInstanceExtensions.union(preferredInstanceExtensions.intersect(availableInstanceExtensions))
        logger.info("Enabling instance extensions: $instanceExtensionsToEnable")

        if(availableInstanceExtensions.contains(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
            debugOutputEnable = true

        val pRequestedInstanceExtensions = stackMallocPointer(requiredExtensions.remaining() + instanceExtensionsToEnable.size)
        pRequestedInstanceExtensions.put(requiredExtensions)
        instanceExtensionsToEnable.forEach { extensionName -> pRequestedInstanceExtensions.put(stackUTF8(extensionName)) }
        pRequestedInstanceExtensions.flip()

        var pRequestedLayers: PointerBuffer? = null
        if (enableValidation) {
            logger.info("Validation layer requested, enabling...")
            pRequestedLayers = stackCallocPointer(1)
            pRequestedLayers.put(stackUTF8("VK_LAYER_KHRONOS_validation"))
            pRequestedLayers.flip()
        }

        val patch = 70
        var vulkan11OrLater = true
        val apiVersion = stackMallocInt(1)
        try {
            apiVersion.clear()
            VK11.vkEnumerateInstanceVersion(apiVersion)
            vulkanVersion = VulkanVersion(VK_VERSION_MAJOR(apiVersion.get(0)), VK_VERSION_MINOR(apiVersion.get(0)), VK_VERSION_PATCH(apiVersion.get(0)))
        } catch(e: NullPointerException) {
            logger.info("Caught NPE calling vkEnumerateInstanceVersion, assuming Vulkan 1.0 instance...")
            vulkanVersion = VulkanVersion(1, 1, patch)
            vulkan11OrLater = false
        }

        val appInfoStruct = VkApplicationInfo.callocStack().sType(VK_STRUCTURE_TYPE_APPLICATION_INFO).apply {
            pApplicationName(stackUTF8("Chunk Stories"))
            pEngineName(stackUTF8("Chunk Stories Vulkan Backend"))

            // Vulkan 1.0 instances cannot deal with forward instance API versions
            if(vulkan11OrLater)
                apiVersion(VK_MAKE_VERSION(vulkanVersion.major, vulkanVersion.minor, patch))
            else
                apiVersion(VK_MAKE_VERSION(1, 0, patch))
        }
        val createInfoStruct = VkInstanceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO).apply {
            pApplicationInfo(appInfoStruct)
            ppEnabledExtensionNames(pRequestedInstanceExtensions)
            ppEnabledLayerNames(pRequestedLayers)
        }

        val pInstance = stackMallocPointer(1)
        vkCreateInstance(createInfoStruct, null, pInstance).let {
            val exceptionMessage = when(it) {
                VK_SUCCESS -> return@let
                VK_ERROR_OUT_OF_HOST_MEMORY -> "Out of host memory (somehow)!"
                VK_ERROR_OUT_OF_DEVICE_MEMORY -> "Out of device memory!"
                VK_ERROR_LAYER_NOT_PRESENT -> "Layer not present"
                VK_ERROR_EXTENSION_NOT_PRESENT -> "A required extension not present, check your graphics hardware is supported and your drivers are up to date"
                VK_ERROR_FEATURE_NOT_PRESENT -> "A required device feature isn't supported, check your graphics hardware is supported and your drivers are up to date"
                VK_ERROR_INCOMPATIBLE_DRIVER -> "Incompatible driver, check your graphics hardware is supported and your drivers are up to date"
                else -> "unknown error (code=$it)"
            }

            throw Exception("Failed to create Vulkan instance: $exceptionMessage")
        }

        val vkInstance = VkInstance(pInstance.get(0), createInfoStruct)
        logger.info("Successfully created Vulkan instance")
        return vkInstance
    }

    private fun setupDebug(vkInstance: VkInstance): Long {
        val jvmCallback = object : VkDebugReportCallbackEXT() {
            override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
                logger.error(getString(pMessage))
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

            val pCallback = stackMallocLong(1)
            vkCreateDebugReportCallbackEXT(vkInstance, dbgSetupStruct, null, pCallback).ensureIs("Failed to create debug callback !", VK_SUCCESS)
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

    // yes this engine is a little tailor-made, what gives
    override fun createVoxelTextures(voxels: Content.Voxels) = VulkanVoxelTexturesArray(this, voxels)

    override fun createWorldRenderer(world: WorldClientCommon) = VulkanWorldRenderer(this, world)

    override fun reloadRendergraph() {
        this.queuedRenderGraph = this.renderGraph.dslCode
    }

    override fun cleanup() {
        vkDeviceWaitIdle(logicalDevice.vkDevice)

        swapchain.flush()
        renderGraph.cleanup()

        textures.cleanup()
        //virtualTexturing.cleanup()
        descriptorMegapool.cleanup()

        vkDestroyRenderPass(logicalDevice.vkDevice, renderToBackbuffer, null)
        swapchain.cleanup()

        frameDataAllocatorProvider.cleanup()
        memoryManager.cleanup()

        logicalDevice.cleanup()

        vkDestroyDebugReportCallbackEXT(instance, debugCallback, null)
        if(enableValidation) {
            if (cookie)
                logger.debug("You get a cookie for not making the validation layer unhappy :)")
            else
                logger.debug("The validation layer found errors, no cookie for you !")
        }

        vkDestroyInstance(instance, null)
        logger.debug("Successfully finished cleaning up Vulkan objects")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger("client.gfx_vk")
    }
}