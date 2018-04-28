//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.entity.components.EntityInventory;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class GiveCommand extends ServerCommandBasic {

	public GiveCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("give").setHandler(this);
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if(!emitter.hasPermission("server.give"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}
		
		Content gameContent = server.getContent();

		Player player = (Player) emitter;

		if (arguments.length == 0) {
			player.sendMessage("#FF969BSyntax : /give <item> [amount] [to]");
			return true;
		}

		int amount = 1;
		Player to = player;

		String itemName = arguments[0];

		// Look for the item first
		ItemDefinition type = gameContent.items().getItemDefinition(itemName);

		// If the type was found we are simply trying to spawn an item
		Item item = null;
		if (type != null)
			item = type.newItem();
		else {
			String voxelName = itemName;
			int voxelMeta = 0;
			if (voxelName.contains(":")) {
				voxelMeta = Integer.parseInt(voxelName.split(":")[1]);
				voxelName = voxelName.split(":")[0];
			}

			// Try to find a matching voxel
			Voxel voxel = gameContent.voxels().getVoxel(itemName);

			if (voxel != null) {
				// Spawn new itemPile in his inventory
				ItemVoxel itemVoxel = (ItemVoxel) gameContent.items().getItemDefinition("item_voxel").newItem();
				itemVoxel.voxel = voxel;
				itemVoxel.voxelMeta = voxelMeta;

				item = itemVoxel;
			}
		}

		if (item == null) {
			player.sendMessage("#FF969BItem or voxel \"" + arguments[0] + " can't be found.");
			return true;
		}

		if (arguments.length >= 2) {
			amount = Integer.parseInt(arguments[1]);
		}
		if (arguments.length >= 3) {
			if(gameContent instanceof ServerInterface)
				to = ((ServerInterface)gameContent).getPlayerByName(arguments[2]);
			else {
				player.sendMessage("#FF969BThis is a singleplayer world - there are no other players");
				return true;
			}
		}
		if (to == null) {
			player.sendMessage("#FF969BPlayer \"" + arguments[2] + " can't be found.");
			return true;
		}
		ItemPile itemPile = new ItemPile(item);
		itemPile.setAmount(amount);

		final int amountFinal = amount;
		final Player to2 = to;
		
		to.getControlledEntity().components.with(EntityInventory.class, ei -> {
			ei.addItemPile(itemPile);
			player.sendMessage("#FF969BGave " + (amountFinal > 1 ? amountFinal + "x " : "" ) + "#4CFF00" + itemPile.getItem().getName() + " #FF969Bto " + to2.getDisplayName());
		});
		
		return true;
	}

}
