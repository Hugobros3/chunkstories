//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

/**
 * Spawns arbitrary entities in the World
 */
public class SpawnEntityCommand extends ServerCommandBasic {

    public SpawnEntityCommand(Server serverConsole) {
        super(serverConsole);
        server.getPluginManager().registerCommand("spawnentity", this);
    }

    // Lazy, why does Java standard lib doesn't have a clean way to do this tho
    // http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
    public static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
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

        if (!emitter.hasPermission("world.spawnEntity")) {
            emitter.sendMessage("You don't have the permission.");
            return true;
        }

        if (arguments.length == 0) {
            emitter.sendMessage("Syntax: /spawnEntity <entityId> [x y z]");
            return false;
        }

        Location spawnLocation = player.getLocation();
        if (arguments.length >= 4) {
            spawnLocation = new Location(player.getWorld(), Double.parseDouble(arguments[1]), Double.parseDouble(arguments[2]),
                    Double.parseDouble(arguments[3]));
        }

        EntityDefinition entityType;

        String TraitName = arguments[0];
        entityType = server.getContent().entities().getEntityDefinition(TraitName);

        if (entityType == null) {
            emitter.sendMessage("Entity type : " + arguments[0] + " not found in loaded content.");
            return true;
        }

        Entity entity = entityType.newEntity(spawnLocation.getWorld());
        entity.traitLocation.set(spawnLocation);

        spawnLocation.getWorld().addEntity(entity);

        emitter.sendMessage("#00FFD0" + "Spawned " + entity.getClass().getSimpleName() + " at "
                + (arguments.length >= 4 ? spawnLocation.toString() : player.getName()));

        return true;
    }

}
