package xyz.chunkstories.graphics.vulkan.util

import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency

@UpdateFrequency(frequency = UniformUpdateFrequency.ONCE_PER_BATCH)
class ShadowMappingInfo(var cascadesCount: Int = 4, val cameras: Array<Camera> = arrayOf(Camera(), Camera(), Camera(), Camera())) : InterfaceBlock {

}