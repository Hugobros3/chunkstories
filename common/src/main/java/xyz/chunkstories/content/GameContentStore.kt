//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.animation.AnimationsStore
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.crafting.RecipesStore
import xyz.chunkstories.entity.EntityDefinitionsStore
import xyz.chunkstories.item.ItemDefinitionsStore
import xyz.chunkstories.localization.LocalizationManagerImplementation
import xyz.chunkstories.loot.LootTablesStore
import xyz.chunkstories.mesh.MeshStore
import xyz.chunkstories.net.PacketsStore
import xyz.chunkstories.particle.ParticlesTypesStore
import xyz.chunkstories.block.BlockTypesStore
import xyz.chunkstories.world.generator.WorldGeneratorsStore

import java.io.File

class GameContentStore(val engine: EngineImplemI, coreContentLocation: File, requestedMods: List<String>) : Content {
    override val modsManager: ModsManagerImplementation = ModsManagerImplementation(coreContentLocation, requestedMods)

    override val lootTables: LootTablesStore = LootTablesStore(this)
    override val items: ItemDefinitionsStore = ItemDefinitionsStore(this)
    override val blockTypes: BlockTypesStore = BlockTypesStore(this)
    override val recipes: RecipesStore = RecipesStore(this)
    override val entities: EntityDefinitionsStore = EntityDefinitionsStore(this)
    val packets: PacketsStore = PacketsStore(this)
    override val particles: ParticlesTypesStore = ParticlesTypesStore(this)
    override val generators: WorldGeneratorsStore = WorldGeneratorsStore(this)

    override val animationsLibrary: AnimationsStore = AnimationsStore(this)
    override val models: MeshStore = MeshStore(this)

    private val localizationManager: LocalizationManagerImplementation = LocalizationManagerImplementation(this, "en")

    override fun reload() {
        try {
            modsManager.loadEnabledMods()
        } catch (e: NotAllModsLoadedException) {
            e.printStackTrace()
        }

        lootTables.reload()
        items.reload()
        blockTypes.reload()
        recipes.reload()
        entities.reload()
        packets.reload()
        particles.reload()
        generators.reload()

        animationsLibrary.reloadAll()

        models.reloadAll()

        localizationManager.reload()
    }

    override fun getAsset(assetName: String): Asset? {
        return modsManager.getAsset(assetName)
    }

    override fun localization(): Content.LocalizationManager {
        return localizationManager
    }

    override val logger: Logger
        get() = contentLogger

    companion object {
        private val contentLogger = LoggerFactory.getLogger("content")
    }
}
