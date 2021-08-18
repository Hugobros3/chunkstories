//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.content.translator

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.net.PacketId
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.net.PacketDefinition
import java.io.*
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashSet

/** Assigns IDs for everything that needs one  */
abstract class AbstractContentTranslator(override val content: GameContentStore) : ContentTranslator {
    override var requiredMods: MutableSet<String> = mutableSetOf()
        protected set

    var voxelMappings: MutableMap<BlockType, Int> = mutableMapOf()
    private lateinit var voxelsArray: Array<BlockType?>

    var entityMappings: MutableMap<EntityDefinition, Int> = mutableMapOf()
    private lateinit var entitiesArray: Array<EntityDefinition?>

    var itemMappings: MutableMap<ItemDefinition, Int> = mutableMapOf()
    private lateinit var itemsArray: Array<ItemDefinition?>

    var packetMappings: MutableMap<PacketDefinition, Int> = mutableMapOf()
    private lateinit var packetsArray: Array<PacketDefinition?>

    fun assignVoxelIds(overwrite: Boolean) {
        if (overwrite)
            voxelMappings = mutableMapOf()
        content.blockTypes.all.forEach { blockType ->
            if (voxelMappings[blockType] == null) {
                if (blockType.name == "air")
                    voxelMappings[blockType] = 0 // Air gets ID 0, always.
                else
                    voxelMappings[blockType] = findNextFreeId(1, voxelMappings.values)
            }
        }
    }

    fun assignEntityIds(overwrite: Boolean) {
        if (overwrite)
            entityMappings = mutableMapOf()
        content.entities.all.forEach {
            entity -> if (entityMappings[entity] == null)
                entityMappings[entity] = findNextFreeId(1, entityMappings!!.values)
        }
    }

    fun assignItemIds(overwrite: Boolean) {
        if (overwrite) itemMappings = mutableMapOf()
        content.items.all.forEach { item ->
            if (overwrite || itemMappings[item] == null)
                itemMappings[item] = findNextFreeId(1, itemMappings.values)
        }
    }

    fun assignPacketIds(overwrite: Boolean) {
        if (overwrite) packetMappings = mutableMapOf()
        content.packets.all.forEach { def ->
            if (overwrite || packetMappings[def] == null) {
                val packetId: Int = def.fixedId
                if (packetId != -1) {
                    packetMappings[def] = packetId
                    logger.debug("Using known assigned id " + packetId + " for " + def.name)
                }
            }
        }
        content.packets.all.forEach{ def ->
            if (overwrite || packetMappings[def] == null) {
                var packetId: Int = def.fixedId
                if (packetId == -1) {
                    packetId = findNextFreeId(0, packetMappings!!.values)
                    packetMappings[def] = packetId
                    logger.debug("Assigned id " + packetId + " to " + def.name)
                }
            }
        }
    }

    private fun findNextFreeId(baseId: Int, values: Collection<Int>): Int {
        var id = baseId
        while (values.contains(id)) {
            id++
        }
        return id
    }

    fun buildArrays() {
        // Create indexable arrays
        voxelsArray = arrayOfNulls(Collections.max(voxelMappings.values) + 1)
        voxelMappings.forEach { (voxel: BlockType, id: Int) -> voxelsArray[id] = voxel }
        entitiesArray = arrayOfNulls(Collections.max(entityMappings.values) + 1)
        entityMappings.forEach { (entity: EntityDefinition?, id: Int?) -> entitiesArray[id] = entity }
        itemsArray = arrayOfNulls(Collections.max(itemMappings.values) + 1)
        itemMappings.forEach { (item, id) -> itemsArray[id] = item }
        if (packetMappings.size > 0) {
            packetsArray = arrayOfNulls(Collections.max(packetMappings.values) + 1)
            packetMappings.forEach { (packet: PacketDefinition?, id: Int?) -> packetsArray[id] = packet }
        } else packetsArray = arrayOfNulls(0)
    }

    /**
     * Derives a modified ContentTranslator that takes into account the new content
     */
    fun loadWith(content: Content?): AbstractContentTranslator? {
        TODO("was this ever used")
    }

    /**
     * Internal method to check if a content has the right mods loaded. Returns the
     * missing mods.
     */
    fun hasRequiredMods(content: Content): Set<String> {
        val loadedMods = content.modsManager.currentlyLoadedMods
        val loadedModsAsString: MutableSet<String> = HashSet()
        loadedMods.forEach {
            mod: Mod -> loadedModsAsString.add(mod.modInfo.internalName)
        }
        val missing: MutableSet<String> = HashSet()
        for (internalName in requiredMods) {
            if (!loadedModsAsString.contains(internalName)) missing.add(internalName)
        }

        return missing
    }

    /**
     * Can we load an existing world with the current configuration without issues ?
     */
    fun compatibleWith(content: GameContentStore): Boolean {
        // Check every needed mod is present
        if (hasRequiredMods(content).isNotEmpty()) return false

        // Check every translatable definition has a match
        for (voxel in voxelMappings.keys) if (content.blockTypes[voxel.name] == null) return false
        for (entity in entityMappings.keys) if (content.entities.getEntityDefinition(entity.name) == null) return false
        for (item in itemMappings.keys) if (content.items.getItemDefinition(item.name) == null) return false
        for (packet in packetMappings.keys) if (content.packets.getPacketByName(packet.name) == null) return false
        return true
    }

    override fun getIdForVoxel(voxel: BlockType): Int {
        return voxelMappings.getOrDefault(voxel, -1)
    }

    override fun getVoxelForId(id: Int): BlockType? {
        return if (id >= 0 && id < voxelsArray.size) voxelsArray[id] else null
    }

    override fun getIdForItem(definition: ItemDefinition): Int {
        return itemMappings.getOrDefault(definition, -1)
    }

    override fun getIdForItem(item: Item?): Int {
        return getIdForItem(item!!.definition)
    }

    override fun getItemForId(id: Int): ItemDefinition? {
        return if (id < 0 || id >= itemsArray.size) null else itemsArray[id]
    }

    override fun getIdForEntity(definition: EntityDefinition): Int {
        return entityMappings.getOrDefault(definition, -1)
    }

    override fun getIdForEntity(entity: Entity): Int {
        return getIdForEntity(entity.definition)
    }

    override fun getEntityForId(id: Int): EntityDefinition? {
        return if (id < 0 || id >= entitiesArray.size) null else entitiesArray[id]
    }

    fun getIdForPacket(definition: PacketDefinition): PacketId {
        val id = packetMappings.getOrDefault(definition, -1)
        if (id == -1) {
            logger.debug("d:$definition")
            packetMappings.forEach { (def, i) ->
                logger.debug(def.name + ":" + def + ":" + i)
            }
        }
        return id
    }

    fun getPacketForId(id: PacketId): PacketDefinition? {
        return if (id < 0 || id >= packetsArray.size) null else packetsArray[id]
    }

    fun write(writer: BufferedWriter, writeOnlineStuff: Boolean) {
        writer.write(toString(writeOnlineStuff))
    }

    override fun toString(): String {
        return toString(true)
    }

    fun toString(writeOnlineStuff: Boolean): String {
        val sb = StringBuilder()
        for (internalName in requiredMods) {
            sb.append("requiredMod $internalName\n")
        }
        for ((key, value) in voxelMappings) {
            sb.append("voxel $value ${key.name}\n")
        }
        for ((key, value) in itemMappings) {
            sb.append("item $value ${key.name}\n")
        }
        for ((key, value) in entityMappings) {
            sb.append("entity $value ${key.name}\n")
        }
        if (writeOnlineStuff) {
            for ((key, value) in packetMappings) {
                sb.append("packet $value ${key.name}\n")
            }
        }
        return sb.toString()
    }

    fun test() {
        try {
            val writer = BufferedWriter(OutputStreamWriter(System.out))
            write(writer, true)
            writer.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun save(file: File) {
        try {
            file.parentFile.mkdirs()
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file)))
            write(writer, false)
            writer.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger("content.translator")
    }

}