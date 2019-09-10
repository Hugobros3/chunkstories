//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.region

import org.slf4j.LoggerFactory
import java.io.IOException

import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.world.io.IOTask
import xyz.chunkstories.world.region.format.RegionFileSerialization

class IOTaskSaveRegion(internal var region: RegionImplementation) : IOTask() {

    public override fun task(taskExecutor: TaskExecutor): Boolean {
        // First compress all loaded chunks !
        region.compressAll()

        try {
            // Create the necessary directory structure if needed
            region.file.parentFile.mkdirs()
            RegionFileSerialization.saveRegion(region.file, region)
        } catch (e: IOException) {
            logger.error("Error writing region $region to ${region.file}: $e")
            e.printStackTrace()
        }

        this.region.eventSavingFinishes()

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger("world.io")
    }
}