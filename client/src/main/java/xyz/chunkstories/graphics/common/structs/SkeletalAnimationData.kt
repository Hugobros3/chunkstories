package xyz.chunkstories.graphics.common.structs

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency

@UpdateFrequency(frequency = UniformUpdateFrequency.ONCE_PER_BATCH)
class SkeletalAnimationData : InterfaceBlock {
    val bones = Array(32) { Matrix4f() }
}