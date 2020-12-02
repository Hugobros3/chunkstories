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
import xyz.chunkstories.PlayerCommon
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.math.LoopingMathHelper
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.packets.PacketOpenInventory
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.server.Server
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

class ServerPlayer(val playerConnection: ClientConnection, name: String) : PlayerCommon(name), RemotePlayer {
    private val server = playerConnection.context

    private val loginTime = System.currentTimeMillis()
    private var serverMetadata = ServerPlayerMetadata()
    private val serverMetadataFile: File

    override val displayName: String
        get() = getUniqueColorPrefix(name) + name + "#FFFFFF"
    override val inputsManager: ServerPlayerInputsManager

    override val subscribedToList: Collection<Entity>
        get() = subscribedEntities

    fun whenEnteringWorld(world: WorldServer) {
        eventEntersWorld(world)

        if (::loadingAgent.isInitialized)
            this.loadingAgent.destroy()

        this.loadingAgent = ServerPlayerLoadingAgent(this, world)

        this.virtualSoundManager = world.soundManager.ServerPlayerVirtualSoundManager(this)
        this.virtualParticlesManager = world.particlesManager.ServerPlayerVirtualParticlesManager(this)
        this.virtualDecalsManager = world.decalsManager.ServerPlayerVirtualDecalsManager(this)
    }

    override var controlledEntity: Entity? = null
        set(newEntity) {
            synchronized(this) {
                val oldEntity = controlledEntity

                if (newEntity == oldEntity)
                    return

                if (oldEntity != null)
                    oldEntity.traits[TraitControllable::class]?.controller = null

                if (newEntity != null)
                    newEntity.traits[TraitControllable::class]?.controller = this

                field = newEntity
            }
        }

    override val uuid
        get() = this.name.hashCode().toLong()

    // Streaming control
    private val subscribedEntities = ConcurrentHashMap.newKeySet<Entity>()

    // Dummy managers to relay synchronisation stuff
    private var virtualSoundManager: ServerPlayerVirtualSoundManager? = null
    private var virtualParticlesManager: ServerPlayerVirtualParticlesManager? = null
    private var virtualDecalsManager: ServerPlayerVirtualDecalsManager? = null

    lateinit var loadingAgent: ServerPlayerLoadingAgent private set

    init {
        File("players/").mkdirs()
        //TODO this should use revised UUIDs
        this.serverMetadataFile = File("players/" + name.toLowerCase() + ".json")
        if (this.serverMetadataFile.exists())
            this.serverMetadata = Gson().fromJson(serverMetadataFile.reader(), ServerPlayerMetadata::class.java)

        this.inputsManager = ServerPlayerInputsManager(this)
    }

    /** Asks the server's permission manager if the player is ok to do that  */
    override fun hasPermission(permissionNode: String): Boolean {
        return playerConnection.context.permissionsManager.hasPermission(this, permissionNode)
    }

    override fun openInventory(inventory: Inventory) {
        val entity = this.controlledEntity
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
        val controlledEntity = this.controlledEntity ?: return

        // Cache (idk if HotSpot makes it redudant but whatever)
        val worldSize = controlledEntity.world.worldSize
        val controlledTraitLocation = controlledEntity.location


        //println(controlledEntity.world.allLoadedChunks.count())
        if (!subscribedEntities.contains(controlledEntity))
            subscribe(controlledEntity)

        val inRangeEntitiesIterator = controlledEntity.world.getEntitiesInBox(Box.Companion.fromExtentsCentered(Vector3d(ENTITY_VISIBILITY_SIZE)).translate(controlledTraitLocation))
        while (inRangeEntitiesIterator.hasNext()) {
            val e = inRangeEntitiesIterator.next()

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
            val dx = LoopingMathHelper.moduloDistance(controlledTraitLocation.x(), loc.x(), worldSize)
            val dy = Math.abs(controlledTraitLocation.y() - loc.y())
            val dz = LoopingMathHelper.moduloDistance(controlledTraitLocation.z(), loc.z(), worldSize)
            val inRange = (dx < ENTITY_VISIBILITY_SIZE && dz < ENTITY_VISIBILITY_SIZE
                    && dy < ENTITY_VISIBILITY_SIZE)

            // System.out.println(inRange);

            // Reasons other than distance to stop tracking this entity
            if (/* !e.shouldBeTrackedBy(this) || */!inRange)
                this.unsubscribe(e)

            // No need to do anything as the component system handles the updates
        }
    }

    override fun subscribe(entity: Entity): Boolean {
        if (subscribedEntities.add(entity)) {
            entity.subscribers.register(this)
            return true
        }
        return false
    }

    override fun unsubscribe(entity: Entity): Boolean {
        if (entity.subscribers.unregister(this)) {
            subscribedEntities.remove(entity)
            return true
        }
        return false
    }

    override fun unsubscribeAll() {
        val iterator = subscribedEntities.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next()
            // If one of the entities is controllable ...
            entity.traits[TraitControllable::class]?.apply {
                if (controller == this)
                    controller = null
            }

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

    override fun flush() {
        this.playerConnection.flush()
    }

    override fun disconnect(disconnectionReason: String) {
        this.playerConnection.disconnect(disconnectionReason)
    }

    fun getContext(): Server {
        return server
    }

    /** Serializes the stuff describing this player (non world-specific) */
    private fun saveMetadata() {
        if (controlledEntity != null) {
            // Useless, kept for admin easyness, scripts, whatnot
            val controlledTraitLocation = controlledEntity!!.location

            // Safely assumes as a SERVER the world will be master ;)
            val world = controlledTraitLocation.world as WorldMaster

            serverMetadata.lastPositionX = controlledTraitLocation.x()
            serverMetadata.lastPositionY = controlledTraitLocation.y()
            serverMetadata.lastPositionZ = controlledTraitLocation.z()
            serverMetadata.worldName = world.worldInfo.internalName
        }

        // Telemetry (zomg so EVIL)
        val now = System.currentTimeMillis()
        serverMetadata.timePlayed += now - loginTime
        serverMetadata.lastLogin = now

        serverMetadataFile.writeText(Gson().toJson(serverMetadata))
        logger.info("Player profile $name saved in $serverMetadataFile")
    }

    override fun toString(): String {
        return name
    }

    fun destroy() {
        val controlledEntity = this.controlledEntity
        if (controlledEntity != null) {
            (controlledEntity.world as WorldImplementation).playersMetadata[this]!!.savedEntity = EntitySerialization.serializeEntity(controlledEntity)
            controlledEntity.world.removeEntity(controlledEntity)
            eventLeavesWorld(controlledEntity.world)
        }
        saveMetadata()
        unsubscribeAll()
        loadingAgent.destroy()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    }
}