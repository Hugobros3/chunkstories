//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.net.packets.PacketDecal;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;

public class VirtualServerDecalsManager implements DecalsManager {
	WorldServer worldServer;

	public VirtualServerDecalsManager(WorldServer worldServer, DedicatedServer server) {
		this.worldServer = worldServer;
	}

	public class ServerPlayerVirtualDecalsManager implements DecalsManager {
		ServerPlayer serverPlayer;

		public ServerPlayerVirtualDecalsManager(ServerPlayer serverPlayer) {
			this.serverPlayer = serverPlayer;
		}

		@Override
		public void add(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName) {
			Iterator<Player> i = worldServer.getPlayers();
			while (i.hasNext()) {
				Player player = i.next();
				if (!player.equals(serverPlayer))
					tellPlayer(player, position, orientation, size, decalName);
			}
		}

	}

	void tellPlayer(Player player, Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName) {
		PacketDecal packet = new PacketDecal(worldServer, decalName, position, orientation, size);
		player.pushPacket(packet);
	}

	@Override
	public void add(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName) {
		Iterator<Player> i = worldServer.getPlayers();
		while (i.hasNext()) {
			Player player = i.next();
			tellPlayer(player, position, orientation, size, decalName);
		}
	}

}
