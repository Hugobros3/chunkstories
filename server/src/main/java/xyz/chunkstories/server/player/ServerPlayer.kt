//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.player

import com.google.gson.Gson
import org.joml.Vector3d
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.RemotePlayer
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.math.MathUtils.mod_dist
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.packets.PacketOpenInventory
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.util.getUniqueColorPrefix
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.server.net.ClientConnection
import xyz.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager
import xyz.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager
import xyz.chunkstories.sound.VirtualSoundManager.ServerPlayerVirtualSoundManager
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldServer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class ServerPlayerMetadata(
        var timePlayed: Long = 0,
        var lastLogin: Long = System.currentTimeMillis(),
        var firstLogin: Long = System.currentTimeMillis(),
        var worldName: String? = null,
        var lastPositionX: Double = 0.0,
        var lastPositionY: Double = 0.0,
        var lastPositionZ: Double = 0.0
)

const val ENTITY_VISIBILITY_SIZE = 192.0

class ServerPlayer(val playerConnection: ClientConnection, override val id: PlayerID, override val name: String) : RemotePlayer {
    private val loginTime = System.currentTimeMillis()
    private var metadata = ServerPlayerMetadata()
    private val file: File

    override val displayName: String
        get() = getUniqueColorPrefix(name) + name + "#FFFFFF"

    fun whenEnteringWorld(world: WorldServer) {
        eventEntersWorld(world)

        if (::loadingAgent.isInitialized)
            this.loadingAgent.destroy()

        this.loadingAgent = ServerPlayerLoadingAgent(this, world)

        this.virtualSoundManager = world.soundManager.ServerPlayerVirtualSoundManager(this)
        this.virtualParticlesManager = world.particlesManager.ServerPlayerVirtualParticlesManager(this)
        this.virtualDecalsManager = world.decalsManager.ServerPlayerVirtualDecalsManager(this)
    }

    // Streaming control
    private val subscribedEntities = ConcurrentHashMap.newKeySet<Entity>()

    // Dummy managers to relay synchronisation stuff
    private var virtualSoundManager: ServerPlayerVirtualSoundManager? = null
    private var virtualParticlesManager: ServerPlayerVirtualParticlesManager? = null
    private var virtualDecalsManager: ServerPlayerVirtualDecalsManager? = null

    lateinit var loadingAgent: ServerPlayerLoadingAgent private set

    init {
        File("players/").mkdirs()
        this.file = File("players/" + id.uuid.toString() + ".json")
        if (this.file.exists())
            this.metadata = Gson().fromJson(file.reader(), ServerPlayerMetadata::class.java)

        //this.inputsManager = ServerPlayerInputsManager(this)
    }

    /** Asks the server's permission manager if the player is ok to do that  */
    override fun hasPermission(permissionNode: String): Boolean {
        return playerConnection.server.permissionsManager.hasPermission(this, permissionNode)
    }

    override fun openInventory(inventory: Inventory) {
        val entity = this.entityIfIngame
        if (entity != null && inventory.isAccessibleTo(entity)) {
            if (inventory.owner is Entity) {
                //TODO have open/close mechanics for inventories
                //val trait = (inventory.owner as Entity).traits.all().find { it is TraitInventory && it.inventory == inventory }!! as TraitInventory
                //trait.pushComponent(this)
            }

            // this.sendMessage("Sending you the open inventory request.");
            val open = PacketOpenInventory(entity.world, inventory)
            this.pushPacket(open)
        }
        // else
        // this.sendMessage("Notice: You don't have access to this inventory.");
    }

    // Entity tracking
    fun updateTrackedEntities() {
        val controlledEntity = this.entityIfIngame ?: return

        val worldSize = (controlledEntity.world.properties.size.squareSizeInBlocks)
        val controlledTraitLocation = controlledEntity.location

        //println(controlledEntity.world.allLoadedChunks.count())
        if (!subscribedEntities.contains(controlledEntity))
            subscribe(controlledEntity)

        val inRangeEntities = controlledEntity.world.getEntitiesInBox(Box.Companion.fromExtentsCentered(Vector3d(ENTITY_VISIBILITY_SIZE)).translate(controlledTraitLocation))
        for (e in inRangeEntities) {
            val shouldTrack = true// e.shouldBeTrackedBy(this);
            val contains = subscribedEntities.contains(e)

            if (shouldTrack && !contains)
                this.subscribe(e)

            if (!shouldTrack && contains)
                this.unsubscribe(e)
        }

        val subscribedEntitiesIterator = subscribedEntities.iterator()
        while (subscribedEntitiesIterator.hasNext()) {
            val e = subscribedEntitiesIterator.next()

            val loc = e.location

            // Distance calculations
            val dx = mod_dist(controlledTraitLocation.x(), loc.x(), worldSize.toDouble())
            val dy = Math.abs(controlledTraitLocation.y() - loc.y())
            val dz = mod_dist(controlledTraitLocation.z(), loc.z(), worldSize.toDouble())
            val inRange = (dx < ENTITY_VISIBILITY_SIZE && dz < ENTITY_VISIBILITY_SIZE
                    && dy < ENTITY_VISIBILITY_SIZE)

            // Reasons other than distance to stop tracking this entity
            if (/* !e.shouldBeTrackedBy(this) || */!inRange)
                this.unsubscribe(e)

            // No need to do anything as the component system handles the updates
        }
    }

    fun subscribe(entity: Entity): Boolean {
        if (subscribedEntities.add(entity)) {
            entity.subscribers.register(this)
            return true
        }
        return false
    }

    fun unsubscribe(entity: Entity): Boolean {
        if (entity.subscribers.unregister(this)) {
            subscribedEntities.remove(entity)
            return true
        }
        return false
    }

    fun unsubscribeAll() {
        val iterator = subscribedEntities.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next()
            // If one of the entities is controllable ...
            entity.controller = null
            entity.subscribers.unregister(this)
            iterator.remove()
        }
    }

    override fun sendMessage(message: String) {
        playerConnection.sendTextMessage("chat/$message")
    }

    override fun pushPacket(packet: Packet) {
        this.playerConnection.pushPacket(packet)
    }

    fun flush() {
        this.playerConnection.flush()
    }

    fun disconnect(disconnectionReason: String) {
        this.playerConnection.disconnect(disconnectionReason)
    }

    /** Serializes the stuff describing this player (non world-specific) */
    private fun saveMetadata() {
        val playerEntity = this.entityIfIngame
        if (playerEntity != null) {
            val controlledTraitLocation = playerEntity.location
            val world = controlledTraitLocation.world
            metadata.lastPositionX = controlledTraitLocation.x()
            metadata.lastPositionY = controlledTraitLocation.y()
            metadata.lastPositionZ = controlledTraitLocation.z()
            metadata.worldName = world.properties.internalName
        }

        // 'Telemetry' (zomg so EVIL)
        val now = System.currentTimeMillis()
        metadata.timePlayed += now - loginTime
        metadata.lastLogin = now

        file.writeText(Gson().toJson(metadata))
        logger.info("Player profile $name saved in $file")
    }

    override fun toString(): String {
        return name
    }

    fun destroy() {
        val playerEntity = this.entityIfIngame
        if (playerEntity != null) {
            (playerEntity.world as WorldImplementation).playersMetadata[id]!!.savedEntity = EntitySerialization.serializeEntity(playerEntity)
            playerEntity.world.removeEntity(playerEntity.id)
            eventLeavesWorld(playerEntity.world)
        }
        saveMetadata()
        unsubscribeAll()
        loadingAgent.destroy()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    }
}