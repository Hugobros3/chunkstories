//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.player

import org.joml.Vector3d
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.input.InputsManager
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.math.LoopingMathHelper
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.packets.PacketOpenInventory
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.entity.SerializedEntityFile
import xyz.chunkstories.server.net.ClientConnection
import xyz.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager
import xyz.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager
import xyz.chunkstories.sound.VirtualSoundManager.ServerPlayerVirtualSoundManager
import xyz.chunkstories.world.WorldServer
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ServerPlayer(val playerConnection: ClientConnection, override val name: String) : RemotePlayer {
    private val server = playerConnection.context

    private val playerMetadata: Properties
    private val playerMetadataFile: File

    override val displayName: String
        get() = ColorsTools.getUniqueColorPrefix(name) + name + "#FFFFFF"
    override val inputsManager: InputsManager
        get() = serverInputsManager

    override val subscribedToList: Collection<Entity>
        get() = subscribedEntities

    fun whenEnteringWorld(world: WorldServer) {
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

    // Mirror of client inputs
    private val serverInputsManager: ServerPlayerInputsManager

    // Dummy managers to relay synchronisation stuff
    private var virtualSoundManager: ServerPlayerVirtualSoundManager? = null
    private var virtualParticlesManager: ServerPlayerVirtualParticlesManager? = null
    private var virtualDecalsManager: ServerPlayerVirtualDecalsManager? = null

    lateinit var loadingAgent: ServerPlayerLoadingAgent private set

    init {

        //TODO this should use revised UUIDs
        File("players/").mkdirs()
        this.playerMetadataFile = File("./players/" + name.toLowerCase() + ".metadata")

        this.playerMetadata = Properties()//OldStyleConfigFile("./players/" + playerName.toLowerCase() + ".cfg");

        this.serverInputsManager = ServerPlayerInputsManager(this)

        // Sets dates
        this.playerMetadata.setProperty("lastlogin", "" + System.currentTimeMillis())
        if (this.playerMetadata.getProperty("firstlogin", "nope") == "nope")
            this.playerMetadata.setProperty("firstlogin", "" + System.currentTimeMillis())
    }

    /** Asks the server's permission manager if the player is ok to do that  */
    override fun hasPermission(permissionNode: String): Boolean {
        return playerConnection.context.permissionsManager.hasPermission(this, permissionNode)
    }

    fun hasSpawned(): Boolean {
        return controlledEntity != null && !controlledEntity!!.traitLocation.wasRemoved()
    }

    override fun openInventory(inventory: Inventory) {
        val entity = this.controlledEntity
        if (entity != null && inventory.isAccessibleTo(entity)) {
            if (inventory.owner is Entity) {
                val trait = (inventory.owner as Entity).traits.all().find { it is TraitInventory && it.inventory == inventory }!! as TraitInventory
                trait.pushComponent(this)
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
        val world_size = controlledEntity.world.worldSize
        val controlledTraitLocation = controlledEntity.location

        val ENTITY_VISIBILITY_SIZE = 192.0

        //println(controlledEntity.world.allLoadedChunks.count())
        if (!subscribedEntities.contains(controlledEntity))
            subscribe(controlledEntity)

        val inRangeEntitiesIterator = controlledEntity.world.getEntitiesInBox(controlledTraitLocation,
                Vector3d(ENTITY_VISIBILITY_SIZE, ENTITY_VISIBILITY_SIZE, ENTITY_VISIBILITY_SIZE))
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
            val dx = LoopingMathHelper.moduloDistance(controlledTraitLocation.x(), loc.x(), world_size)
            val dy = Math.abs(controlledTraitLocation.y() - loc.y())
            val dz = LoopingMathHelper.moduloDistance(controlledTraitLocation.z(), loc.z(), world_size)
            val inRange = (dx < ENTITY_VISIBILITY_SIZE && dz < ENTITY_VISIBILITY_SIZE
                    && dy < ENTITY_VISIBILITY_SIZE)

            // System.out.println(inRange);

            // Reasons other than distance to stop tracking this entity
            if (/* !e.shouldBeTrackedBy(this) || */!inRange)
                this.unsubscribe(e)

            // No need to do anything as the component system handles the updates
        }
    }

    fun getSubscribedToList(): MutableIterator<Entity> {
        return subscribedEntities.iterator()
    }

    override fun subscribe(entity: Entity): Boolean {
        if (subscribedEntities.add(entity)) {
            entity.subscribers.register(this)

            // Only the server should ever push all components to a client
            entity.traits.all().forEach { c ->
                if (c is TraitSerializable)
                    c.pushComponent(this)
            }
            return true
        }
        return false
    }

    override fun unsubscribe(entity: Entity): Boolean {
        // Thread.dumpStack();
        // System.out.println("sub4sub");
        if (entity.subscribers.unregister(this))
        // TODO REMOVE ENTITY EXISTENCE COMPONENT IT'S STUPID AND WRONG
        {
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

    fun isSubscribedTo(entity: Entity): Boolean {
        return subscribedEntities.contains(entity)
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

    /** Serializes the stuff describing this player  */
    fun save() {
        val lastTime = playerMetadata.getProperty("timeplayed")?.toLong() ?: 0L
        val lastLogin = playerMetadata.getProperty("lastlogin").toLong()

        if (controlledEntity != null) {
            // Useless, kept for admin easyness, scripts, whatnot
            val controlledTraitLocation = controlledEntity!!.location

            // Safely assumes as a SERVER the world will be master ;)
            val world = controlledTraitLocation.world as WorldMaster

            playerMetadata.setProperty("posX", controlledTraitLocation.x().toString())
            playerMetadata.setProperty("posY", controlledTraitLocation.y().toString())
            playerMetadata.setProperty("posZ", controlledTraitLocation.z().toString())
            playerMetadata.setProperty("world", world.worldInfo.internalName)

            // Serializes the whole player entity !!!
            val playerEntityFile = SerializedEntityFile(
                    world.folderPath + "/players/" + this.name.toLowerCase() + ".csf")
            playerEntityFile.write(controlledEntity)
        }

        // Telemetry (zomg so EVIL)
        playerMetadata.setProperty("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)))
        try {
            playerMetadata.store(FileWriter(playerMetadataFile), "Metadata file for player$name")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        logger.info("Player profile $name saved.")
    }

    override fun toString(): String {
        return name
    }

    fun destroy() {
        save()
        if (controlledEntity != null) {
            controlledEntity?.world?.removeEntity(controlledEntity!!)
        }
        unsubscribeAll()
        loadingAgent.destroy()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    }
}
