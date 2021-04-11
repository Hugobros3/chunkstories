//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.propagation;

import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager;
import org.joml.Vector3dc;

import xyz.chunkstories.net.packets.PacketDecal;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.server.DedicatedServer;
import xyz.chunkstories.server.player.ServerPlayer;
import xyz.chunkstories.world.WorldServer;

public class VirtualServerDecalsManager implements DecalsManager {
	private final WorldServer worldServer;

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
			VirtualServerDecalsManager.this.add(position, orientation, size, decalName);
		}

	}

	@Override
	public void add(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName) {
		for (Player player : worldServer.getPlayers()) {
			PacketDecal packet = new PacketDecal(worldServer, decalName, position, orientation, size);
			player.pushPacket(packet);
		}
	}

}
