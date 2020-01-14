package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import java.nio.ByteBuffer
import java.util.*

/** Generic interface for obtaining scratch buffers for per-frame data, has multiple per-vendor implementations for "optimal" usage */
interface FrameDataAllocator {
    /** Gets a byte buffer whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getByteBuffer(size: Long): ByteBuffer

    /** Returns a reserved offset into an UBO whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getUBO(size: Long): Pair<VulkanBuffer, Long>

    /** Overloads getUBO to also populate the buffer immediately with some data */
    fun getUBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long>

    /** Overloads getUBO but this time the buffer isn't consumed right away to allow the memory operations to be merged */
    fun getUBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long>

    /** Returns a reserved offset into a SSBO whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getSSBO(size: Long): Pair<VulkanBuffer, Long>

    /** Overloads getSSBO to also populate the buffer immediately with some data */
    fun getSSBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long>

    /** Overloads getSSBO but this time the buffer isn't consumed right away to allow the memory operations to be merged */
    fun getSSBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long>
}

internal interface PerFrameDataProvider {
    fun beginFrame(frame: VulkanFrame): FrameDataAllocator

    fun retireFrame(frame: VulkanFrame)
}

class NaivePerFrameDataProvider(val backend: VulkanGraphicsBackend) : PerFrameDataProvider {
    private val maxFramesInFlight = backend.swapchain.maxFramesInFlight
    private val deque: Deque<NaivePerFrameData> = ArrayDeque()

    override fun beginFrame(frame: VulkanFrame): FrameDataAllocator {
        val frameData = NaivePerFrameData(frame)
        deque.addLast(frameData)
        return frameData
    }

    override fun retireFrame(frame: VulkanFrame) {
        val id = frame.frameNumber
        val popped = deque.removeFirst()
        if(popped.frame.frameNumber != id)
            throw Exception("Ring buffer assumption failed :/")

        popped.cleanup()
    }

    inner class NaivePerFrameData(val frame: VulkanFrame) : FrameDataAllocator, Cleanable {
        override fun getByteBuffer(size: Long): ByteBuffer {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getUBO(size: Long): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getUBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getUBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getSSBO(size: Long): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getSSBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getSSBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun cleanup() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

abstract class GenericPerFrameDataProvider : PerFrameDataProvider

/** Uses that sweet 256MiB direct window to GPU memory on AMD hardware */
abstract class AMD_Gcn_PerFrameDataProvider : PerFrameDataProvider