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
import xyz.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor
import xyz.chunkstories.api.world.World
import xyz.chunkstories.input.InputVirtual
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Transfers client's input to the server
 */
class PacketInput(world: World) : PacketWorld(world) {
    var input: Input? = null
    var isPressed: Boolean = false

    @Throws(IOException::class)
    override fun send(destinator: PacketDestinator, out: DataOutputStream, ctx: PacketSendingContext) {
        out.writeLong(input!!.hash)

        out.writeBoolean(isPressed)
    }

    @Throws(IOException::class)
    override fun process(sender: PacketSender, dis: DataInputStream, processor: PacketReceptionContext) {
        val code = dis.readLong()
        val pressed = dis.readBoolean()

        if (processor is ServerPlayerPacketsProcessor) {

            // Look for the controller handling this buisness
            val entity = processor.player.controlledEntity

            if (entity != null) {
                // Get input of the client
                val input = processor.player.inputsManager.getInputFromHash(code) as? InputVirtual ?: throw NullPointerException("Unknown input hash : $code")
                input.isPressed = pressed

                // Fire appropriate event
                if (pressed) {
                    val event = PlayerInputPressedEvent(processor.player, input!!)
                    entity.world.gameLogic.pluginsManager.fireEvent(event)

                    if (!event.isCancelled)
                        entity.traits[TraitControllable::class]?.let {
                            it.onControllerInput(input)
                        }
                } else {
                    val event = PlayerInputReleasedEvent(processor.player, input!!)
                    entity.world.gameLogic.pluginsManager.fireEvent(event)
                }
            }
        }
    }
}
