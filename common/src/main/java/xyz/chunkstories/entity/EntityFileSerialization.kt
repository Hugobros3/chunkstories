//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.json.stringSerialize
import xyz.chunkstories.api.content.json.toJson
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.world.World
import java.io.File

object EntityFileSerialization {
    fun readEntityFromDisk(file: File, world: World): Entity? {
        if(!file.exists())
            return null

        try {
            val contents = file.readText()
            val entity = EntitySerialization.deserializeEntity(world, contents.toJson())
            return entity
        } catch (e: Exception) {
            logger.warn("Failed to load entity from $file ($e)")
            return null
        }
    }

    fun writeEntityToDisk(file: File, entity: Entity) {
        try {
            file.parentFile.mkdirs()

            val json = EntitySerialization.serializeEntity(entity)
            file.writeText(json.stringSerialize())

            logger.info("Wrote serialized entity to $file")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val logger = LoggerFactory.getLogger("world.serialization.entity")
}
