//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net

import java.util.HashSet

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.RemoteServer

/**
 * Represents the Remote server from the client point of view. See RemoteServer
 * in the API for more details on the concept behind this.
 */
class RemoteServerImplementation(internal val connection: ServerConnection) : RemoteServer {

    internal var controlledEntity: MutableSet<Entity> = HashSet(1)

    override val subscribedToList: Collection<Entity>
        get() = controlledEntity

    override val uuid: Long
        get() = -1

    override fun subscribe(entity: Entity): Boolean {
        assert(controlledEntity.size == 0)
        entity.subscribers.register(this)
        return controlledEntity.add(entity)
    }

    override fun unsubscribe(entity: Entity): Boolean {
        assert(controlledEntity.size == 1)
        entity.subscribers.unregister(this)
        return controlledEntity.remove(entity)
    }

    override fun unsubscribeAll() {
        throw UnsupportedOperationException()
    }

    override fun pushPacket(packet: Packet) {
        this.connection.pushPacket(packet)
    }

    override fun flush() {
        this.connection.flush()
    }

    override fun disconnect() {
        this.connection.close()
    }

    override fun disconnect(disconnectionReason: String) {
        // TODO send reason if possible
        this.connection.close()
    }
}
