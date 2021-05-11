//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.File

import org.junit.Test

import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException
import xyz.chunkstories.content.mods.ModsManagerImplementation

class ModsManagerTest {

    @Test
    fun testModsManager() {
        //TODO don't assume that/try other possible locations
        val coreContentLocation = System.getProperty("coreContentLocation", "../chunkstories-core/build/distributions/core_content.zip")

        try {
            val modsManager = ModsManagerImplementation(File(coreContentLocation), emptyList())
            modsManager.loadEnabledMods()
        } catch (e: NotAllModsLoadedException) {
            e.printStackTrace()
        }

    }
}
