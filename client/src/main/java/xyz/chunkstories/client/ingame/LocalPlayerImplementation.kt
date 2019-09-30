//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.ingame

import xyz.chunkstories.PlayerCommon
import xyz.chunkstories.api.client.ClientInputsManager
import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.graphics.Window
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.WorldClientNetworkedRemote
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.sound.ALSoundManager
import xyz.chunkstories.world.WorldClientCommon

class LocalPlayerImplementation(override val client: IngameClientImplementation, internal val world: WorldClientCommon) : PlayerCommon(client.user.name), LocalPlayer {

    val loadingAgent: LocalClientLoadingAgent = LocalClientLoadingAgent(client, this, world)

    override val displayName: String
        get() = name
    override val inputsManager: ClientInputsManager
        get() = client.inputsManager
    override val subscribedToList: Collection<Entity>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val uuid: Long
        get() = client.user.name.hashCode().toLong()
    override val window: Window
        get() = client.gameWindow

    override var controlledEntity: Entity? = null
        set(new) {
            val old = field

            val ec = new?.traits?.get(TraitControllable::class.java)
            if (new != null && ec != null) {
                this.subscribe(new)

                // If a world master, directly set the entity's controller
                if (world is WorldMaster)
                    ec.controller = this
                else if (new.world is WorldClientNetworkedRemote) {
                    // When changing controlled entity, first unsubscribe the remote server from the
                    // one we no longer own
                    if (old != null && new !== old)
                        (old.world as WorldClientNetworkedRemote).remoteServer.unsubscribe(old)

                    // Let know the server of new changes
                    (new.world as WorldClientNetworkedRemote).remoteServer.subscribe(new)
                }// In remote networked worlds, we need to subscribe the server to our player
                // actions to the controlled entity so he gets updates

                field = new
            } else if (new == null && old != null) {
                // Directly unset it
                if (world is WorldMaster)
                    old.traits[TraitControllable::class]?.let { it.controller = null }
                else if (old.world is WorldClientNetworkedRemote)
                    (old.world as WorldClientNetworkedRemote).remoteServer
                            .unsubscribe(old)// When loosing control over an entity, stop sending the server updates about it

                field = null
            }

            return
        }

    init {
        eventEntersWorld(world)
    }

    fun update() {
        loadingAgent.updateUsedWorldBits()

        controlledEntity?.let { entity ->
            val camera = entity.traits[TraitControllable::class]?.camera
            camera?.let {
                (client.soundManager as ALSoundManager).setListenerPosition(camera.position.toVec3f(), camera.lookingAt, camera.up)
            }
        }
    }

    override fun subscribe(entity: Entity): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun unsubscribe(entity: Entity): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun unsubscribeAll() {
        // TODO Auto-generated method stub

    }

    override fun pushPacket(packet: Packet) {
        // TODO Auto-generated method stub

    }

    override fun hasFocus(): Boolean {
        return client.gui.hasFocus() && inputsManager.mouse.isGrabbed
    }

    override fun sendMessage(msg: String) {
        client.print(msg)
    }

    override fun hasPermission(permissionNode: String): Boolean {
        return true
    }

    override fun flush() {
        // TODO Auto-generated method stub

    }

    override fun openInventory(inventory: Inventory) {
        val entity = this.controlledEntity
        if (entity != null && inventory.isAccessibleTo(entity)) {
            val playerInventory = entity.traits[TraitInventory::class.java]?.inventory

            if (playerInventory != null)
                client.gui.openInventories(playerInventory, inventory)
            else
                client.gui.openInventories(inventory)
        }
    }

    fun destroy() {
        val playerWorldMetadata = world.playersMetadata[this]!!
        val controlledEntity1 = controlledEntity
        if (controlledEntity1 != null) {
            playerWorldMetadata.savedEntity = EntitySerialization.serializeEntity(controlledEntity1)
            this.controlledEntity = null
            //val playerEntityFile = File(world.folderPath + "/players/" + this.name.toLowerCase() + ".json")
            //EntityFileSerialization.writeEntityToDisk(playerEntityFile, playerEntity)
        }
        eventLeavesWorld(world)
        loadingAgent.unloadEverything(true)
    }
}
