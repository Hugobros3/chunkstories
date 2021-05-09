//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.content.translator

import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.content.GameContentStore

class InitialContentTranslator(content: GameContentStore) : AbstractContentTranslator(content) {
    init {
        requiredMods = mutableSetOf()
        content.modsManager.currentlyLoadedMods.forEach { m -> requiredMods.add(m.modInfo.internalName) }
        assignVoxelIds(true)
        assignEntityIds(true)
        assignItemIds(true)
        assignPacketIds(true)
        buildArrays()
    }
}