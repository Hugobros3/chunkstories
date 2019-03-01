package xyz.chunkstories.graphics.common

import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.world.WorldClientCommon

abstract class WorldRenderer(val world: WorldClientCommon): Cleanable {

}