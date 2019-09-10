//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.voxel.components

import xyz.chunkstories.api.voxel.components.VoxelComponent
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.cell.CellComponents
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.world.chunk.ChunkImplementation
import java.util.*
import kotlin.collections.Map.Entry

class CellComponentsHolder(override val chunk: ChunkImplementation, val index: Int) : CellComponents {

    internal var map: MutableMap<String, VoxelComponent> = HashMap()

    override val x: Int
        get() {
            return chunk.chunkX * 32 + index / 1024
        }

    override val y: Int
        get() {
            return chunk.chunkY * 32 + index / 32 % 32
        }

    override val z: Int
        get() {
            return chunk.chunkZ * 32 + index % 32
        }

    fun erase() {
        chunk.removeComponents(index)
    }

    fun put(name: String, component: VoxelComponent) {
        map[name] = component
    }

    override fun getVoxelComponent(name: String): VoxelComponent? {
        return map[name]
    }

    override val allVoxelComponents: Collection<Entry<String, VoxelComponent>>
        get() = map.entries

    override val world: World
        get() {
            return chunk.world
        }

    override val cell: ChunkCell
        get() {
            return chunk.peek(x, y, z)
        }

    fun users(): Set<WorldUser> {
        return chunk.holder().users
    }

    override fun getRegisteredComponentName(component: VoxelComponent): String? {
        // Reverse lookup
        return allVoxelComponents.find { it.value == component }?.key
    }
}
