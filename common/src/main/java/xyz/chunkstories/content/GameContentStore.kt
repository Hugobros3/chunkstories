//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.animation.BVHLibrary
import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.content.mods.ModsManagerImplementation.NonExistentCoreContent
import xyz.chunkstories.entity.EntityDefinitionsStore
import xyz.chunkstories.item.ItemDefinitionsStore
import xyz.chunkstories.localization.LocalizationManagerImplementation
import xyz.chunkstories.mesh.MeshStore
import xyz.chunkstories.net.PacketsStore
import xyz.chunkstories.particle.ParticlesTypesStore
import xyz.chunkstories.voxel.VoxelsStore
import xyz.chunkstories.world.generator.WorldGeneratorsStore

import java.io.File

class GameContentStore(override val context: GameContext, coreContentLocation: File, enabledModsLaunchArguments: String) : Content {
    private val modsManager: ModsManager

    private val items: ItemDefinitionsStore
    private val voxels: VoxelsStore
    private val entities: EntityDefinitionsStore
    private val packets: PacketsStore
    private val particles: ParticlesTypesStore
    private val generators: WorldGeneratorsStore

    override val animationsLibrary: BVHLibrary
    override val models: MeshStore

    private val localizationManager: LocalizationManagerImplementation

    init {
        try {
            this.modsManager = ModsManagerImplementation(coreContentLocation, enabledModsLaunchArguments)
        } catch (e: NonExistentCoreContent) {
            logger().error("Could not find core content at the location: " + coreContentLocation.absolutePath)
            throw RuntimeException("Could not find core content at the location: " + coreContentLocation.absolutePath)
        }

        items = ItemDefinitionsStore(this)
        voxels = VoxelsStore(this)
        entities = EntityDefinitionsStore(this)
        packets = PacketsStore(this)
        particles = ParticlesTypesStore(this)
        generators = WorldGeneratorsStore(this)

        animationsLibrary = BVHLibrary(this)

        models = MeshStore(this)

        localizationManager = LocalizationManagerImplementation(this, "en")
    }

    override fun reload() {
        try {
            modsManager.loadEnabledMods()
        } catch (e: NotAllModsLoadedException) {
            e.printStackTrace()
        }

        items.reload()
        voxels.reload()
        entities.reload()
        packets.reload()
        particles.reload()
        generators.reload()

        animationsLibrary.reloadAll()

        models.reloadAll()

        localizationManager.reload()
    }

    override fun voxels(): VoxelsStore {
        return voxels
    }

    override fun items(): ItemDefinitionsStore {
        return items
    }

    override fun entities(): EntityDefinitionsStore {
        return entities
    }

    override fun particles(): ParticlesTypesStore {
        return particles
    }

    override fun packets(): PacketsStore {
        return packets
    }

    override fun modsManager(): ModsManager {
        return modsManager
    }

    override fun getAsset(assetName: String): Asset? {
        return modsManager.getAsset(assetName)
    }

    override fun generators(): WorldGeneratorsStore {
        return generators
    }

    override fun localization(): Content.LocalizationManager {
        return localizationManager
    }

    override fun logger(): Logger {
        return contentLogger
    }

    companion object {
        private val contentLogger = LoggerFactory.getLogger("content")
    }
}
