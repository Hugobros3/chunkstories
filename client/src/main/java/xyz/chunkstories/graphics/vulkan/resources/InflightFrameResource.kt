package xyz.chunkstories.graphics.vulkan.resources

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame

/** Declares a resource type that is instanced N times as there are N frames in flight */
class InflightFrameResource<R : Any>(val backend: VulkanGraphicsBackend, val autoRecreate:Boolean = true, val cleanupFunction: ((R) -> Unit)? = null, val initLambda: () -> R) {

    init {
        init(backend.swapchain.maxFramesInFlight)

        if(autoRecreate)
            backend.swapchain.listeners.add(this)
    }

    private fun init(size: Int) {
        stackPush()
        values = Array<Any>(size) { initLambda() }
        stackPop()
    }

    fun whenSwapchainSizeChanges(size: Int) {
        cleanupInternal()
        init(size)
    }

    lateinit var values: Array<Any>

    operator fun get(frame: VulkanFrame) = values[frame.frameNumber % values.size] as R

    fun cleanupInternal() {
        if (cleanupFunction != null)
            values.forEach { cleanupFunction.invoke((it as R)) }
        else
            values.forEach { (it as? Cleanable)?.cleanup() }
    }

    fun cleanup() {
        if(autoRecreate)
             backend.swapchain.listeners.remove(this)
        cleanupInternal()
    }
}

