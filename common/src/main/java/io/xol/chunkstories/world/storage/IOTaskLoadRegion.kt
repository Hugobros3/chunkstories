//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.storage

import java.io.DataInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

import org.slf4j.LoggerFactory

import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.world.io.IOTask

class IOTaskLoadRegion(private val region: RegionImplementation) : IOTask() {

    public override fun task(taskExecutor: TaskExecutor): Boolean {
        if (region.file!!.exists()) {
            try {
                val fist = FileInputStream(region.handler!!.file)
                val inputStream = DataInputStream(fist)

                region.handler.load(inputStream)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                logger.warn("Error loading file " + region.handler!!.file)
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