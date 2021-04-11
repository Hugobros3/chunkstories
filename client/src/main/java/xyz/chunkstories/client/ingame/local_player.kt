//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.ingame

import xyz.chunkstories.api.graphics.structs.camera
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.util.kotlin.toVec3f

class LocalPlayerImplementation(val client: IngameClientImplementation) : Player {

    override val id: PlayerID
        get() = client.user.id
    override val displayName: String
        get() = name

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

    init {
        eventEntersWorld(client.world)
    }

    fun update() {
        loadingAgent.updateUsedWorldBits()

        val camera = client.camera
        client.soundManager.setListenerPosition(camera.position.toVec3f(), camera.lookingAt, camera.up)
    }

    /*override fun hasFocus(): Boolean {
        return client.gui.hasFocus() // && inputsManager.mouse.isGrabbed
    }*/

    override fun sendMessage(msg: String) {
        client.print(msg)
    }

    override fun hasPermission(permissionNode: String): Boolean {
        return true
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

    fun destroy() {
        eventLeavesWorld(client.world)
        loadingAgent.unloadEverything(true)
    }
}
