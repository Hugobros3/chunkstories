//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.player

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager
import org.joml.Vector3d

import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.entity.traits.serializable.TraitController
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory
import io.xol.chunkstories.api.entity.traits.serializable.TraitSerializable
import io.xol.chunkstories.api.input.InputsManager
import io.xol.chunkstories.api.item.inventory.Inventory
import io.xol.chunkstories.api.math.LoopingMathHelper
import io.xol.chunkstories.api.net.Packet
import io.xol.chunkstories.api.net.packets.PacketOpenInventory
import io.xol.chunkstories.api.particles.ParticlesManager
import io.xol.chunkstories.api.server.RemotePlayer
import io.xol.chunkstories.api.server.Server
import io.xol.chunkstories.api.sound.SoundManager
import io.xol.chunkstories.api.util.ColorsTools
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.entity.SerializedEntityFile
import io.xol.chunkstories.server.ServerInputsManager
import io.xol.chunkstories.server.net.ClientConnection
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager
import io.xol.chunkstories.sound.VirtualSoundManager.ServerPlayerVirtualSoundManager
import io.xol.chunkstories.world.WorldServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ServerPlayer(val playerConnection: ClientConnection, private val playerName: String) : RemotePlayer {
    protected val server: Server

    private val playerMetadata: Properties
    private val playerMetadataFile: File

    private var world: WorldServer? = null
    private var controlledEntity: Entity? = null

    // Streaming control
    private val subscribedEntities = ConcurrentHashMap.newKeySet<Entity>()

    // Mirror of client inputs
    private val serverInputsManager: ServerInputsManager

    // Dummy managers to relay synchronisation stuff
    private var virtualSoundManager: ServerPlayerVirtualSoundManager? = null
    private var virtualParticlesManager: ServerPlayerVirtualParticlesManager? = null
    private var virtualDecalsManager: ServerPlayerVirtualDecalsManager? = null

    val loadingAgent = RemotePlayerLoadingAgent(this)

    val lastPosition: Location?
        get() = if (this.playerMetadata.containsKey("posX")) {
            Location(getWorld(), playerMetadata.getProperty("posX").toDouble(), playerMetadata.getProperty("posY").toDouble(),
                    playerMetadata.getProperty("posZ").toDouble())
        } else null

    init {

        this.server = playerConnection.context

        //TODO this should use revised UUIDs
        this.playerMetadataFile = File("./players/" + playerName.toLowerCase() + ".metadata")
        this.playerMetadata = Properties()//OldStyleConfigFile("./players/" + playerName.toLowerCase() + ".cfg");

        this.serverInputsManager = ServerInputsManager(this)

        // Sets dates
        this.playerMetadata.setProperty("lastlogin", "" + System.currentTimeMillis())
        if (this.playerMetadata.getProperty("firstlogin", "nope") == "nope")
            this.playerMetadata.setProperty("firstlogin", "" + System.currentTimeMillis())
    }

    override fun getName() = playerName

    override fun getUUID(): Long {
        // TODO make them longer
        return this.name.hashCode().toLong()
    }

    /** Hashes the player name so he gets a nice color :) */
    override fun getDisplayName(): String {
        return ColorsTools.getUniqueColorPrefix(name) + name + "#FFFFFF"
    }

    /** Asks the server's permission manager if the player is ok to do that  */
    override fun hasPermission(permissionNode: String): Boolean {
        return playerConnection.context.permissionsManager.hasPermission(this, permissionNode)
    }

    override fun getWorld(): WorldServer? {
        return world
    }

    fun setWorld(world: WorldServer) {
        this.world = world

        this.virtualSoundManager = world.soundManager.ServerPlayerVirtualSoundManager(this)
        this.virtualParticlesManager = world.particlesManager.ServerPlayerVirtualParticlesManager(this)
        this.virtualDecalsManager = world.decalsManager.ServerPlayerVirtualDecalsManager(this)
    }

    override fun hasSpawned(): Boolean {
        return controlledEntity != null && !controlledEntity!!.traitLocation.wasRemoved()
    }

    override fun getLocation(): Location? {
        return if (controlledEntity != null) controlledEntity!!.location else null
    }

    override fun setLocation(l: Location) {
        if (this.controlledEntity != null)
            this.controlledEntity!!.traitLocation.set(l)
    }

    fun removeEntityFromWorld() {
        if (controlledEntity != null) {
            getWorld()!!.removeEntity(controlledEntity!!)
        }
        unsubscribeAll()
    }

    override fun getControlledEntity(): Entity? {
        return controlledEntity
    }

    override fun setControlledEntity(newEntity: Entity?): Boolean {
        synchronized(this) {
            val oldEntity = controlledEntity

            if(newEntity == oldEntity)
                return false

            if(oldEntity != null)
                oldEntity.traits[TraitController::class]?.controller = null

            if(newEntity != null)
                newEntity.traits[TraitController::class]?.controller = this

            controlledEntity = newEntity
        }
        return true
    }

    override fun openInventory(inventory: Inventory) {
        val entity = this.getControlledEntity()
        if (inventory.isAccessibleTo(entity)) {
            if (inventory is TraitInventory) {
                inventory.pushComponent(this)
            }

            // this.sendMessage("Sending you the open inventory request.");
            val open = PacketOpenInventory(getWorld(), inventory)
            this.pushPacket(open)
        }
        // else
        // this.sendMessage("Notice: You don't have access to this inventory.");
    }

    // Entity tracking
    fun updateTrackedEntities() {
        val controlledEntity = this.controlledEntity ?: return

        // Cache (idk if HotSpot makes it redudant but whatever)
        val world_size = controlledEntity.getWorld().worldSize
        val controlledTraitLocation = controlledEntity.location

        val ENTITY_VISIBILITY_SIZE = 192.0

        val inRangeEntitiesIterator = controlledEntity.getWorld().getEntitiesInBox(controlledTraitLocation,
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

    override fun getSubscribedToList(): MutableIterator<Entity> {
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
        val iterator = subscribedToList
        while (iterator.hasNext()) {
            val entity = iterator.next()
            // If one of the entities is controllable ...
            entity.traits[TraitController::class]?.apply {
                if(controller == this)
                    controller = null
            }

            entity.subscribers.unregister(this)
            iterator.remove()
        }
    }

    fun isSubscribedTo(entity: Entity): Boolean {
        return subscribedEntities.contains(entity)
    }

    // Various subsystems managers
    override fun getInputsManager(): InputsManager {
        return serverInputsManager
    }

    override fun getSoundManager(): SoundManager? {
        return virtualSoundManager
    }

    override fun getParticlesManager(): ParticlesManager? {
        return virtualParticlesManager
    }

    override fun getDecalsManager(): DecalsManager? {
        return virtualDecalsManager
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

    override fun disconnect() {
        this.playerConnection.disconnect()
    }

    override fun disconnect(disconnectionReason: String) {
        this.playerConnection.disconnect(disconnectionReason)
    }

    override fun isConnected(): Boolean {
        return playerConnection.isOpen
    }

    override fun getContext(): Server {
        return server
    }

    /** Serializes the stuff describing this player  */
    fun save() {
        val lastTime = playerMetadata.getProperty("timeplayed").toLong()
        val lastLogin = playerMetadata.getProperty("lastlogin").toLong()

        if (controlledEntity != null) {
            // Useless, kept for admin easyness, scripts, whatnot
            val controlledTraitLocation = controlledEntity!!.location

            // Safely assumes as a SERVER the world will be master ;)
            val world = controlledTraitLocation.getWorld() as WorldMaster

            playerMetadata.setProperty("posX", controlledTraitLocation.x().toString())
            playerMetadata.setProperty("posY", controlledTraitLocation.y().toString())
            playerMetadata.setProperty("posZ", controlledTraitLocation.z().toString())
            playerMetadata.setProperty("world", world.worldInfo.internalName)

            // Serializes the whole player entity !!!
            val playerEntityFile = SerializedEntityFile(
                    world.folderPath + "/players/" + this.getName().toLowerCase() + ".csf")
            playerEntityFile.write(controlledEntity)
        }

        // Telemetry (zomg so EVIL)
        playerMetadata.setProperty("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)))
        try {
            playerMetadata.store(FileWriter(playerMetadataFile), "Metadata file for player$playerName")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        logger.info("Player profile $playerName saved.")
    }

    override fun toString(): String {
        return getName()
    }

    fun destroy() {
        save()
        removeEntityFromWorld()

        loadingAgent.destroy()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    }
}
