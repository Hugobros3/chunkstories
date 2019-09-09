//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.serialization.OfflineSerializedData

class SerializedEntityFile(string: String) : OfflineSerializedData {
    private val file: File

    init {
        file = File(string)
    }

    fun exists(): Boolean {
        return file.exists()
    }

    override fun toString(): String {
        return "[CSF File: $file]"
    }

    fun read(world: World): Entity? {
        try {
            val dis = DataInputStream(FileInputStream(file))

            val entity = EntitySerializerOld.readEntityFromStream(dis, this, world)

            dis.close()

            return entity
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    fun write(entity: Entity) {
        try {
            file.parentFile.mkdirs()

            val out = DataOutputStream(FileOutputStream(file))

            EntitySerializerOld.writeEntityToStream(out, this, entity)

            out.flush()
            out.close()

            println("Wrote serialized entity to : $file")
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
