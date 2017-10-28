package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.net.packets.PacketDecal;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VirtualServerDecalsManager implements DecalsManager
{
	WorldServer worldServer;

	public VirtualServerDecalsManager(WorldServer worldServer, DedicatedServer server)
	{
		this.worldServer = worldServer;
	}
	
	public class ServerPlayerVirtualDecalsManager implements DecalsManager
	{
		ServerPlayer serverPlayer;

		public ServerPlayerVirtualDecalsManager(ServerPlayer serverPlayer)
		{
			this.serverPlayer = serverPlayer;
		}

		@Override
		public void drawDecal(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName)
		{
			Iterator<Player> i = worldServer.getPlayers();
			while(i.hasNext())
			{
				Player player = i.next();
				if(!player.equals(serverPlayer))
					tellPlayer(player, position, orientation, size, decalName);
			}
		}

	}
	
	void tellPlayer(Player player, Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName)
	{
		PacketDecal packet = new PacketDecal(decalName, position, orientation, size);
		
		/*packet.decalName = decalName;
		packet.position = position;
		packet.orientation = orientation;
		packet.size = size;*/
		
		player.pushPacket(packet);
	}
	
	@Override
	public void drawDecal(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName)
	{
		Iterator<Player> i = worldServer.getPlayers();
		while(i.hasNext())
		{
			Player player = i.next();
			tellPlayer(player, position, orientation, size, decalName);
		}
	}

}
