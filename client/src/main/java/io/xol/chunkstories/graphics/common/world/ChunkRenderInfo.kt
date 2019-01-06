package io.xol.chunkstories.graphics.common.world

import io.xol.chunkstories.api.graphics.UniformInput
import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.api.graphics.structs.UniformUpdateFrequency
import io.xol.chunkstories.api.graphics.structs.UpdateFrequency

@UpdateFrequency(UniformUpdateFrequency.ONCE_PER_BATCH)
class ChunkRenderInfo : InterfaceBlock {
    var chunkX = 0
    var chunkY = 0
    var chunkZ = 0
}