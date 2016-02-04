package io.xol.chunkstories.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityNameable;
import io.xol.chunkstories.entity.EntityRotateable;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.net.packets.Packet04Entity;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerPlayer implements Player
{
	ConfigFile playerData;
	ServerClient playerConnection;

	public EntityPlayer entity;

	public Map<int[], Long> loadedChunks = new HashMap<int[], Long>();
	public List<Entity> trackedEntities = new ArrayList<Entity>();
	
	public boolean hasSpawned = false;

	public ServerPlayer(ServerClient serverClient)
	{
		playerConnection = serverClient;
		playerData = new ConfigFile(getConfigFilePath());
		// Sets dates
		playerData.setProp("lastlogin", "" + System.currentTimeMillis());
		if (playerData.getProp("firstlogin", "nope").equals("nope"))
			playerData.setProp("firstlogin", "" + System.currentTimeMillis());

		entity = new EntityPlayer(Server.getInstance().world, playerData.getDoubleProp("posX"), playerData.getDoubleProp("posY", 100), playerData.getDoubleProp("posZ"), playerConnection.name);
		//System.out.println(entity.getName()+":"+playerConnection.name);
	}

	public void updateTrackedEntities()
	{
		//System.out.println("edgy");
		if(entity == null)
			return;
		//System.out.println("edgyy");
		Iterator<Entity> iter = entity.world.entities.iterator();
		Entity e;
		double ws = entity.world.getSizeSide();
		boolean shouldTrack = false;
		while(iter.hasNext())
		{
			e = iter.next();
			if(!e.equals(entity))
			{
				double dx = LoopingMathHelper.moduloDistance(entity.posX, e.posX, ws);
				double dy = Math.abs(entity.posX - e.posX);
				double dz = LoopingMathHelper.moduloDistance(entity.posZ, e.posZ, ws);
				shouldTrack = (dx < 256 && dz < 256 && dy < 256);
				boolean contains = trackedEntities.contains(e);
				//System.out.println("[TRACKER] "+e+" shouldTrack:"+shouldTrack+" contains:"+contains+" "+this.playerConnection.name);
				if(shouldTrack && !contains)
				{
					trackedEntities.add(e);
					trackEntity(e, true, false);
				}
				if(!shouldTrack && contains)
				{
					//Despawn the entity
					trackEntity(e, false, true);
					trackedEntities.remove(e);
				}
			}
		}
		iter = trackedEntities.iterator();
		while(iter.hasNext())
		{
			e = iter.next();
			if(e.deleteFlag)
			{
				//Despawn the entity
				trackEntity(e, false, true);
				iter.remove();
				//trackedEntities.remove(e);
			}
			else
			{
				// Just send new positions
				trackEntity(e, false, false);
			}
		}
		//System.out.println("edguuuy");
	}
	
	public void trackEntity(Entity e, boolean first, boolean delete)
	{
		Packet04Entity packet = new Packet04Entity(false);
		packet.entity = e;
		if(first)
		{
			if(e instanceof EntityNameable)
				packet.includeName = true;
		}
		packet.includeRotation = e instanceof EntityRotateable;
		packet.deleteFlag = delete;
		playerConnection.sendPacket(packet);
	}
	
	private String getConfigFilePath()
	{
		return "players/" + playerConnection.name + ".cfg";
	}

	public void save()
	{
		long lastTime = Long.parseLong(playerData.getProp("timeplayed", "0"));
		long lastLogin = Long.parseLong(playerData.getProp("lastlogin", "0"));
		playerData.setProp("posX", entity.posX);
		playerData.setProp("posY", entity.posY);
		playerData.setProp("posZ", entity.posZ);
		playerData.setProp("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerData.save();
		System.out.println("Player profile "+playerConnection.name+" saved.");
	}

	public void onJoin()
	{
		if(entity != null)
		{
			Server.getInstance().world.addEntity(entity);
			System.out.println("spawned player entity");
		}
		
	}
	
	public void onLeave()
	{
		if(entity != null)
		{
			Server.getInstance().world.removeEntity(entity);
			System.out.println("removed player entity");
		}
	}

	@Override
	public String getName()
	{
		return playerConnection.name;
	}

	@Override
	public Entity getControlledEntity()
	{
		return entity;
	}

	@Override
	public void sendTextMessage(String msg)
	{
		playerConnection.sendChat(msg);
	}

	@Override
	public Location getPosition()
	{
		if(entity != null)
			return entity.getLocation();
		return null;
	}

	@Override
	public void setPosition(Location l)
	{
		//Send teleport packet
		Packet04Entity packet = new Packet04Entity(false);
		playerConnection.sendPacket(packet);
		entity.setPosition(l.x, l.y, l.z);
	}

	@Override
	public boolean isConnected()
	{
		return true;
	}

	@Override
	public void kickPlayer(String reason)
	{
		Server.getInstance().handler.disconnectClient(playerConnection, reason);
	}

}
