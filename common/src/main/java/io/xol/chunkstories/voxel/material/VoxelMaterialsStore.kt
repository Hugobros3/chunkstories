//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.material

import DefinitionsLexer
import DefinitionsParser
import java.util.HashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.voxel.materials.VoxelMaterial
import io.xol.chunkstories.content.GameContentStore
import io.xol.chunkstories.util.format.toMap
import io.xol.chunkstories.voxel.VoxelsStore
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

class VoxelMaterialsStore(private val voxels: VoxelsStore) : Content.Voxels.VoxelMaterials {
    private val store: GameContentStore

    internal var materials: MutableMap<String, VoxelMaterial> = HashMap()

    override val defaultMaterial: VoxelMaterial
        get() = null!!

    override fun logger(): Logger {
        return logger
    }

    init {
        this.store = voxels.parent()
    }

    fun reload() {
        materials.clear()

        fun readDefinitions(a: Asset) {
            logger().debug("Reading voxel material definitions in : $a")

            val text = a.reader().use { it.readText() }
            val parser = DefinitionsParser(CommonTokenStream(DefinitionsLexer(ANTLRInputStream(text))))

            for(definition in parser.voxelMaterialDefinitions().voxelMaterialDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val voxelMaterial = VoxelMaterial(this, name, properties)
                materials.put(name, voxelMaterial)

                logger.debug("Loaded voxel material $voxelMaterial")
            }
        }

        val i = store.modsManager().getAllAssetsByExtension("materials")
        while (i.hasNext()) {
            val f = i.next()
            readDefinitions(f)
        }
    }

    override fun getVoxelMaterial(name: String): VoxelMaterial {
        val material = materials[name]
        return material ?: getVoxelMaterial("undefined")

    }

    override fun all(): Iterator<VoxelMaterial> {
        return materials.values.iterator()
    }

    override fun parent(): Content {
        return store
    }

    companion object {

        private val logger = LoggerFactory.getLogger("content.materials")
    }
}
