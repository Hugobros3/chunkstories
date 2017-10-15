package io.xol.chunkstories.server.commands.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Spawns arbitrary entities in the World */
public class SpawnEntityCommand extends ServerCommandBasic {

	public SpawnEntityCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("spawnentity").setHandler(this);
	}
	
	// Lazy, why does Java standard lib doesn't have a clean way to do this tho
	// http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	public static boolean isNumeric(String str)
	{
	    for (char c : str.toCharArray())
	    {
	        if (!Character.isDigit(c)) return false;
	    }
	    return true;
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;
		
		if(!emitter.hasPermission("world.spawnEntity"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		
		if(arguments.length == 0)
		{
			emitter.sendMessage("Syntax: /spawnEntity <entityId> [x y z]");
			return false;
		}
		
		Location loc = player.getLocation();
		if(arguments.length >= 4)
		{
			loc = new Location(player.getWorld(), Double.parseDouble(arguments[1]), Double.parseDouble(arguments[2]), Double.parseDouble(arguments[3]));
		}
		
		EntityType entityType;
		
		if(isNumeric(arguments[0]))
		{
			short entityId = Short.parseShort(arguments[0]);
			entityType = server.getContent().entities().getEntityTypeById(entityId);
		}
		else
		{
			String entityName = arguments[0];
			entityType = server.getContent().entities().getEntityTypeByName(entityName);
		}
		
		if(entityType == null)
		{
			emitter.sendMessage("Entity type : "+arguments[0]+" not found in loaded content.");
			return true;
		}
		
		Entity entity = entityType.create(loc);
		entity.setLocation(loc);
		
		loc.getWorld().addEntity(entity);

		emitter.sendMessage("#00FFD0" + "Spawned " + entity.getClass().getSimpleName() + " at "+(arguments.length >= 4 ? loc.toString() : player.getName()));
		
		return true;
	}

}
