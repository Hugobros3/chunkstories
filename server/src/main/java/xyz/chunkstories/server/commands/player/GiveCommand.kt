//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import java.lang.Integer.min

class GiveCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("give", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (!emitter.hasPermission("server.give")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val gameContent = host.content

        if (arguments.isEmpty()) {
            emitter.sendMessage("#FF969BSyntax : /give <item> [amount] [to]")
            return true
        }

        var amount = 1
        var targetPlayer: Player? = emitter

        val itemName = arguments[0]

        // Look for the item first
        val type = gameContent.items.getItemDefinition(itemName)

        // If the type was found we are simply trying to spawn an item
        var item: Item? = null
        if (type != null)
            item = type.newItem()

        if (item == null) {
            emitter.sendMessage("#FF969BItem or voxel \"" + arguments[0] + " can't be found.")
            return true
        }

        if (arguments.size >= 2) {
            amount = Integer.parseInt(arguments[1])
        }
        if (arguments.size >= 3) {
            if (gameContent is Host)
                targetPlayer = (gameContent as Host).getPlayer(arguments[2])
            else {
                emitter.sendMessage("#FF969BThis is a singleplayer world - there are no other players")
                return true
            }
        }
        if (targetPlayer == null) {
            emitter.sendMessage("#FF969BPlayer \"" + arguments[2] + " can't be found.")
            return true
        }

        val amountFinal = min(amount, item.definition.maxStackSize)

        targetPlayer.entityIfIngame?.traits?.get(TraitInventory::class)?.inventory?.apply {
            addItem(item, amountFinal)
            emitter.sendMessage("#FF969BGave " + (if (amountFinal > 1) amountFinal.toString() + "x " else "") + "#4CFF00" + item.name + " #FF969Bto " + targetPlayer.displayName)
        }

        return true
    }

}
