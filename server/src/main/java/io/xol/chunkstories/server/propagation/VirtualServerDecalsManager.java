//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.propagation;

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.net.packets.PacketDecal;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;

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
