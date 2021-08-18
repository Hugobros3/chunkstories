//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.content.translator

import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.api.content.mods.Mod
import java.io.*
import java.util.function.Consumer

class LoadedContentTranslator(content: GameContentStore, reader: BufferedReader) : AbstractContentTranslator(content) {
    private fun failIfNull(o: Any?, err: String) {
        if (o == null) throw IncompatibleContentException(err)
    }

    companion object {
        fun loadFromFile(content: GameContentStore, file: File): LoadedContentTranslator {
            val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
            return LoadedContentTranslator(content, reader)
        }
    }

    init {
        requiredMods = mutableSetOf()
        voxelMappings = mutableMapOf()
        entityMappings = mutableMapOf()
        itemMappings = mutableMapOf()
        packetMappings = mutableMapOf()
        var line: String = ""
        while (reader.readLine().let { line = it ?: return@let false ; true }) {
            val tokens = line.split(" ").toTypedArray()
            if (line.startsWith("#")) continue

            // 3-tokens lines are mappings
            if (tokens.size == 3) {
                val defType = tokens[0]
                val id = tokens[1].toInt()
                val defName = tokens[2]
                when (defType) {
                    "voxel", "block" -> {
                        val voxel = content.blockTypes[defName]
                        failIfNull(voxel, "Missing voxel definition $defName")
                        voxelMappings[voxel!!] = id
                    }
                    "entity" -> {
                        val entityDef = content.entities.getEntityDefinition(defName)
                        failIfNull(entityDef, "Missing entity definition $defName")
                        entityMappings[entityDef!!] = id
                    }
                    "item" -> {
                        val itemDef = content.items.getItemDefinition(defName)
                        failIfNull(itemDef, "Missing item definition $defName")
                        itemMappings[itemDef!!] = id
                    }
                    "packet" -> {
                        val packetDef = content.packets.getPacketByName(defName)
                        failIfNull(packetDef, "Missing packet definition $defName")
                        packetMappings[packetDef!!] = id
                    }
                    else -> logger.warn("Unknown definition type '" + defType
                            + "' while parsing existing ContentTranslator. Ignoring.")
                }
            } else if (tokens.size == 2) {
                val tokenName = tokens[0]
                val tokenValue = tokens[1]
                when (tokenName) {
                    "requiredMod" -> requiredMods.add(tokenValue)
                    else -> logger.warn(
                            "Unknown token '$tokenName' while parsing existing ContentTranslator. Ignoring.")
                }
            }
        }

        // Ensure the mods situation is okay too
        val missingMods = hasRequiredMods(content)
        if (missingMods.isNotEmpty()) throw IncompatibleContentException("Missing mods: $missingMods")

        // Assign ids to whatever was added
        assignVoxelIds(false)
        assignEntityIds(false)
        assignItemIds(false)
        assignPacketIds(false)
        content.modsManager.currentlyLoadedMods.forEach(Consumer { m: Mod -> requiredMods.add(m.modInfo.internalName) })
        buildArrays()
    }
}