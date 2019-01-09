package xyz.chunkstories.graphics.common.world

import xyz.chunkstories.api.graphics.UniformInput
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency

@UpdateFrequency(UniformUpdateFrequency.ONCE_PER_BATCH)
class ChunkRenderInfo : InterfaceBlock {
    var chunkX = 0
    var chunkY = 0
    var chunkZ = 0
}