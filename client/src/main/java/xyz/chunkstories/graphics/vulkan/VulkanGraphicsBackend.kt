package xyz.chunkstories.graphics.vulkan

import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.vulkan.devices.LogicalDevice
import xyz.chunkstories.graphics.vulkan.devices.PhysicalDevice
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderGraph
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.memory.VulkanMemoryManager
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import xyz.chunkstories.graphics.vulkan.swapchain.SwapChain
import xyz.chunkstories.graphics.vulkan.swapchain.WindowSurface
import xyz.chunkstories.graphics.vulkan.systems.*
import xyz.chunkstories.graphics.vulkan.systems.debug.VulkanDebugDrawer
import xyz.chunkstories.graphics.vulkan.systems.gui.VulkanGuiDrawer
import xyz.chunkstories.graphics.vulkan.textures.VulkanTextures
import xyz.chunkstories.graphics.vulkan.util.*
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.*
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.systems.debug.VulkanSpinningCubeDrawer
import xyz.chunkstories.graphics.vulkan.systems.lighting.VulkanDefferedLightsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanLinesDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanModelsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanSpritesDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.VulkanChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.farterrain.VulkanFarTerrainRenderer
import xyz.chunkstories.graphics.vulkan.textures.voxels.VulkanVoxelTexturesArray
import xyz.chunkstories.graphics.vulkan.world.VulkanWorldRenderer
import xyz.chunkstories.voxel.VoxelTexturesSupport
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage

class VulkanGraphicsBackend(graphicsEngine: GraphicsEngineImplementation, window: GLFWWindow) : GLFWBasedGraphicsBackend(graphicsEngine, window), VoxelTexturesSupport {
    internal val enableValidation = window.client.arguments["enableValidation"] == "true"

    val requiredInstanceExtensions = listOf(VK_EXT_DEBUG_REPORT_EXTENSION_NAME, "VK_KHR_get_physical_device_properties2")
    val requiredDeviceExtensions = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME, "VK_KHR_get_memory_requirements2", "VK_KHR_dedicated_allocation")

    internal var instance: VkInstance
    private val debugCallback: Long
    private var cookie = true

    /** All the physical devices available to Vulkan */
    private val physicalDevices: List<PhysicalDevice>

    /** The physical device in use by the application */
    val physicalDevice: PhysicalDevice

    /** The logical device in use by the application, derived from the physicalDevice */
    val logicalDevice: LogicalDevice

    val memoryManager: VulkanMemoryManager

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

        val requiredExtensions = glfwGetRequiredInstanceExtensions() ?: throw Exception("Vulkan is not supported for windowed rendering on this machine.")

        instance = createVkInstance(requiredExtensions)
        debugCallback = setupDebug(instance)
        surface = WindowSurface(instance, window)

        physicalDevices = enumeratePhysicalDevices()
        physicalDevice = pickPhysicalDevice(true)

        logicalDevice = LogicalDevice(this, physicalDevice)

        shaderFactory = VulkanShaderFactory(this, window.client, logicalDevice)
        memoryManager = VulkanMemoryManager(this, logicalDevice)

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
        val appInfoStruct = VkApplicationInfo.callocStack().sType(VK_STRUCTURE_TYPE_APPLICATION_INFO).apply {
            pApplicationName(MemoryStack.stackUTF8("Chunk Stories"))
            pEngineName(MemoryStack.stackUTF8("Chunk Stories Vulkan Backend"))

            apiVersion(VK_MAKE_VERSION(1, 1, 70))
        }

        val additionalInstanceExtensions = requiredInstanceExtensions.toMutableList()

        val pRequestedInstanceExtensions = MemoryStack.stackMallocPointer(requiredExtensions.remaining() + additionalInstanceExtensions.size)
        pRequestedInstanceExtensions.put(requiredExtensions)
        additionalInstanceExtensions.forEach { extensionName -> pRequestedInstanceExtensions.put(stackUTF8(extensionName)) }
        pRequestedInstanceExtensions.flip()

        var pRequestedLayers: PointerBuffer? = null
        if (enableValidation) {
            logger.info("Validation layer enabled")
            pRequestedLayers = MemoryStack.stackCallocPointer(1)
            pRequestedLayers.put(MemoryStack.stackUTF8("VK_LAYER_LUNARG_standard_validation"))
            pRequestedLayers.flip()
        }

        val createInfoStruct = VkInstanceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO).apply {
            pApplicationInfo(appInfoStruct)
            ppEnabledExtensionNames(pRequestedInstanceExtensions)
            ppEnabledLayerNames(pRequestedLayers)
        }

        val pInstance = MemoryStack.stackMallocPointer(1)
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

    // TODO move to dedicated class
    fun <T : DrawingSystem> createDrawingSystem(pass: VulkanPass, registration: RegisteredGraphicSystem<T>): VulkanDrawingSystem {
        val dslCode = registration.dslCode as DrawingSystem.() -> Unit

        return when (registration.clazz) {
            GuiDrawer::class.java -> VulkanGuiDrawer(pass, window.client.gui)
            FullscreenQuadDrawer::class.java -> VulkanFullscreenQuadDrawer(pass, dslCode)
            FarTerrainDrawer::class.java -> VulkanFarTerrainRenderer(pass, dslCode)

            Vulkan3DVoxelRaytracer::class.java -> Vulkan3DVoxelRaytracer(pass, dslCode)
            VulkanSpinningCubeDrawer::class.java -> VulkanSpinningCubeDrawer(pass, dslCode)
            VulkanDebugDrawer::class.java -> VulkanDebugDrawer(pass, dslCode, window.client.ingame!!)

            else -> throw Exception("Unimplemented system on this backend: ${registration.clazz}")
        }
    }

    // TODO move to dedicated class
    fun <T: DispatchingSystem> getOrCreateDispatchingSystem(list: MutableList<VulkanDispatchingSystem<*,*>>, dispatchingSystemRegistration: RegisteredGraphicSystem<T>): VulkanDispatchingSystem<*,*> {
        val implemClass =  when(dispatchingSystemRegistration.clazz) {
            ChunksRenderer::class.java -> VulkanChunkRepresentationsDispatcher::class
            ModelsRenderer::class.java -> VulkanModelsDispatcher::class
            SpritesRenderer::class.java -> VulkanSpritesDispatcher::class
            LinesRenderer::class.java -> VulkanLinesDispatcher::class
            DefferedLightsRenderer::class.java -> VulkanDefferedLightsDispatcher::class
            else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
        }.java

        val existing = list.find { implemClass.isAssignableFrom(it::class.java) }
        if(existing != null)
            return existing

        //val new = implemClass.getConstructor(VulkanGraphicsBackend::class.java).newInstance(this)
        val new = when(dispatchingSystemRegistration.clazz) {
            ChunksRenderer::class.java -> VulkanChunkRepresentationsDispatcher(this)
            ModelsRenderer::class.java -> VulkanModelsDispatcher(this)
            SpritesRenderer::class.java -> VulkanSpritesDispatcher(this)
            LinesRenderer::class.java -> VulkanLinesDispatcher(this)
            DefferedLightsRenderer::class.java -> VulkanDefferedLightsDispatcher(this)
            else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
        }

        list.add(new)

        return new
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
        val logger = LoggerFactory.getLogger("client.gfx_vk")
    }
}