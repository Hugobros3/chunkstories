//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.ingame

import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.structs.camera
import xyz.chunkstories.api.input.InputsManager
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.player.PlayerState
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldMasterImplementation
import xyz.chunkstories.world.playerLeaves

class ClientPlayer(val ingame: IngameClientImplementation) : Player {
    override var state: PlayerState = PlayerState.None

    override val name: String
        get() = ingame.client.user.name
    override val id: PlayerID
        get() = ingame.client.user.id
    override val displayName: String
        get() = name
    override val inputsManager: InputsManager
        get() = ingame.client.inputsManager

    val world: WorldImplementation
        get() = ingame.world

    /*override var controlledEntity: Entity? = null
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
        }*/

    fun onFrame() {
        val camera = ingame.camera
        ingame.client.soundManager.setListenerPosition(camera.position.toVec3f(), camera.lookingAt, camera.up)

        val playerEntity = entityIfIngame
        if (playerEntity != null)
            playerEntity.traits[TraitControllable::class]?.onEachFrame()
    }

    fun onTick() {
        ingame.loadingAgent.updateUsedWorldBits()
    }

    fun hasFocus(): Boolean {
        return ingame.client.gui.hasFocus() && ingame.client.inputsManager.mouse.isGrabbed
    }

    override fun sendMessage(message: String) {
        ingame.print(message)
    }

    override fun hasPermission(permissionNode: String): Boolean {
        return true
    }

    fun leaveWorld() {
        val world = world as? WorldMasterImplementation ?: return // for remote world it doesn't matter - it gets nuked either way
        val playerEntity = this.entityIfIngame
        if (playerEntity != null) {
            assert(playerEntity.world == world)
            world.playersMetadata[id]!!.savedEntity = EntitySerialization.serializeEntity(playerEntity)
            world.removeEntity(playerEntity.id)
        }
        world.playerLeaves(this)
    }

    fun destroy() {
        leaveWorld()
    }

    /*override fun openInventory(inventory: Inventory) {
        val entity = this.controlledEntity
        if (entity != null && inventory.isAccessibleTo(entity)) {
            val playerInventory = entity.traits[TraitInventory::class.java]?.inventory

            if (playerInventory != null)
                client.gui.openInventories(playerInventory, inventory)
            else
                client.gui.openInventories(inventory)
        }
    }*/
}
