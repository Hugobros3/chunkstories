//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.events.player.PlayerInputPressedEvent
import xyz.chunkstories.api.events.player.PlayerInputReleasedEvent
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.net.*
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.input.InputVirtual
import java.io.DataInputStream
import java.io.DataOutputStream

class PacketInput(world: World) : PacketWorld(world) {
    lateinit var input: Input
    var isPressed: Boolean = false

    override fun send(dos: DataOutputStream) {
        dos.writeLong(input.hash)
        dos.writeBoolean(isPressed)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        val code = dis.readLong()
        val pressed = dis.readBoolean()

        if (world is WorldMaster && player != null) {
            val input = player.inputsManager.getInputFromHash(code) as? InputVirtual ?: throw NullPointerException("Unknown input hash : $code")
            input.isPressed = pressed

            val entity = player.entityIfIngame ?: return

            // Fire appropriate event
            if (pressed) {
                val event = PlayerInputPressedEvent(player, input)
                entity.world.gameInstance.pluginManager.fireEvent(event)

                if (!event.isCancelled)
                    entity.traits[TraitControllable::class]?.onControllerInput(input)
            } else {
                val event = PlayerInputReleasedEvent(player, input)
                entity.world.gameInstance.pluginManager.fireEvent(event)
            }

        }
    }
}
