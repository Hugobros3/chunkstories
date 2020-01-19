package xyz.chunkstories.graphics.vulkan.resources.frameallocator

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock

class NaiveFrameDataAllocatorProvider(val backend: VulkanGraphicsBackend) : FrameDataAllocatorProvider {
    //private val maxFramesInFlight = backend.swapchain.maxFramesInFlight
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

    override fun cleanup() {
        // nothing ... in theory !
        if(deque.isNotEmpty()) {
            throw Exception("Should definitely be empty.")
        }
    }

    private data class UploadRequest(val bb: ByteBuffer, val target: VulkanBuffer)

    inner class NaivePerFrameData(val frame: VulkanFrame) : FrameDataAllocator, Cleanable {
        private val allocatedByteBuffers = ConcurrentLinkedDeque<ByteBuffer>()
        private val allocatedBuffers = ConcurrentLinkedDeque<VulkanBuffer>()

        private val uploadRequests = mutableListOf<UploadRequest>()

        override fun getByteBuffer(size: Long): ByteBuffer {
            val bb = memAlloc(size.toInt())
            allocatedByteBuffers.add(bb)
            return bb
        }

        override fun getUBO(size: Long): Pair<VulkanBuffer, Long> {
            val ubo = VulkanBuffer(backend, size, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
            allocatedBuffers.add(ubo)
            return Pair(ubo, 0L)
        }

        override fun getUBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getMappedUBO(size: Long): Pair<ByteBuffer, Pair<VulkanBuffer, Long>> {
            val bb = memAlloc(size.toInt())
            val ubo = VulkanBuffer(backend, size, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
            uploadRequests.add(UploadRequest(bb, ubo))
            return Pair(bb, Pair(ubo, 0L))
        }

        override fun getSSBO(size: Long): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getSSBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun beforeSubmission() {
            for((src, target) in uploadRequests) {
                if(src.limit() == 0)
                    throw Exception("CANT")
                target.upload(src, 0, src.limit().toLong())
            }

            println("naive per frame data report: allocated ${allocatedByteBuffers.size} temp buffers, ${allocatedBuffers.size} vkbuffers, ${uploadRequests.size} combined upload requests")
        }

        override fun cleanup() {
            allocatedByteBuffers.forEach { memFree(it) }
            allocatedBuffers.forEach(Cleanable::cleanup)

            uploadRequests.forEach { it.target.cleanup() }
        }
    }
}