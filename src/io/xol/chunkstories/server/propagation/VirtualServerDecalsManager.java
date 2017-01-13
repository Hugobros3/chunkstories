package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.net.packets.PacketDecal;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.RemoteServerPlayer;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VirtualServerDecalsManager implements DecalsManager
{
	WorldServer worldServer;

	public VirtualServerDecalsManager(WorldServer worldServer, Server server)
	{
		this.worldServer = worldServer;
	}
	
	public class ServerPlayerVirtualDecalsManager implements DecalsManager
	{
		RemoteServerPlayer serverPlayer;

		public ServerPlayerVirtualDecalsManager(RemoteServerPlayer serverPlayer)
		{
			this.serverPlayer = serverPlayer;
		}

		@Override
		public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName)
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
	
	void tellPlayer(Player player, Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName)
	{
		PacketDecal packet = new PacketDecal();
		
		packet.decalName = decalName;
		packet.position = position;
		packet.orientation = orientation;
		packet.size = size;
		
		player.pushPacket(packet);
	}
	
	@Override
	public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName)
	{
		Iterator<Player> i = worldServer.getPlayers();
		while(i.hasNext())
		{
			Player player = i.next();
			tellPlayer(player, position, orientation, size, decalName);
		}
	}

}
