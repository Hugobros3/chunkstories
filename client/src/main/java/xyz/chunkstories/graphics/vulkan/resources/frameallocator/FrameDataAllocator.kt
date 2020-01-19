package xyz.chunkstories.graphics.vulkan.resources.frameallocator

import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import java.nio.ByteBuffer

/** Generic interface for obtaining scratch buffers for per-frame data, has multiple per-vendor implementations for "optimal" usage */
interface FrameDataAllocator {
    /** Gets a byte buffer whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getByteBuffer(size: Long): ByteBuffer

    /** Returns a reserved offset into an UBO whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getUBO(size: Long): Pair<VulkanBuffer, Long>

    /** Overloads getUBO to also populate the buffer immediately with some data */
    fun getUBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long>

    /** Overloads getUBO to return an associated ByteBuffer that will be uploaded at some time before the frame starts drawing */
    fun getMappedUBO(size: Long): Pair<ByteBuffer, Pair<VulkanBuffer, Long>>

    ///** Overloads getUBO but this time the buffer isn't consumed right away to allow the memory operations to be merged */
    //fun getUBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long>

    /** Returns a reserved offset into a SSBO whose lifetime is tied to the current frame (it gets destroyed when the frame is done rendering */
    fun getSSBO(size: Long): Pair<VulkanBuffer, Long>

    /** Overloads getSSBO to also populate the buffer immediately with some data */
    fun getSSBO(buffer: ByteBuffer): Pair<VulkanBuffer, Long>

    ///** Overloads getSSBO but this time the buffer isn't consumed right away to allow the memory operations to be merged */
    //fun getSSBOUploadWhenever(buffer: ByteBuffer, callback: (() -> Unit)?): Pair<VulkanBuffer, Long>
    fun beforeSubmission()
}