//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.region

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.world.io.IOTask
import xyz.chunkstories.world.region.format.RegionFileSerialization

class IOTaskLoadRegion(private val region: RegionImplementation) : IOTask() {

    public override fun task(taskExecutor: TaskExecutor): Boolean {
        if (region.file.exists()) {
            try {
                RegionFileSerialization.loadRegion(region.file, region)
            } catch (e: Exception) {
                logger.error("Error reading region $region to ${region.file}: $e")
                e.printStackTrace()
            }
        }

        region.eventLoadingFinishes()
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger("world.io")
    }
}