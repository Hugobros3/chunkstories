package xyz.chunkstories.graphics.vulkan.systems.models

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency

@UpdateFrequency(frequency = UniformUpdateFrequency.ONCE_PER_BATCH)
class ExperimentalBonesData : InterfaceBlock {
    val bones = Array(32) { Matrix4f() }
}