package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush

/** Declares a resource type that is instanced N times as there are N frames in flight */
class InflightFrameResource<R : Any>(val backend: VulkanGraphicsBackend, val initLambda: () -> R) {

    init {
        init()
    }

    fun init() {
        stackPush()
        values = Array<Any>(backend.swapchain.maxFramesInFlight) { initLambda() }
        stackPop()
    }

    lateinit var values : Array<Any>

    operator fun get(frame: Frame) = values[frame.inflightFrameIndex] as R

    fun cleanup() {
        values.forEach { (it as? Cleanable)?.cleanup() }
    }

    fun cleanup(cleanupFunction: (R) -> Unit) {
        values.forEach { cleanupFunction((it as R)) }
    }
}

