package io.xol.chunkstories.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.entity.Controller;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityNameable;
import io.xol.chunkstories.entity.EntityRotateable;
import io.xol.chunkstories.net.packets.Packet04Entity;
import io.xol.chunkstories.net.packets.Packet05SerializedInventory;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerPlayer implements Player, Controller
{
	ConfigFile playerData;
	ServerClient playerConnection;

	//Entity controlled
	public Entity controlledEntity;

	//Streaming control
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
		//Does not create a player entity yet, this is taken care of by the player spawn req
	}

	public void updateTrackedEntities()
	{
		//System.out.println("edgy");
		if(controlledEntity == null)
			return;
		//System.out.println("edgyy");
		Iterator<Entity> iter = controlledEntity.world.entities.iterator();
		Entity e;
		double ws = controlledEntity.world.getSizeSide();
		boolean shouldTrack = false;
		while(iter.hasNext())
		{
			e = iter.next();
			if(!e.equals(controlledEntity))
			{
				double dx = LoopingMathHelper.moduloDistance(controlledEntity.posX, e.posX, ws);
				double dy = Math.abs(controlledEntity.posX - e.posX);
				double dz = LoopingMathHelper.moduloDistance(controlledEntity.posZ, e.posZ, ws);
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
			if(e.mpSendDeletePacket)
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
		//First time tracking we send the name if there's one
		if(first)
		{
			if(e instanceof EntityNameable)
				packet.includeName = true;
		}
		packet.includeRotation = e instanceof EntityRotateable;
		packet.deleteFlag = delete;
		packet.applyFromEntity(e);
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
		if(controlledEntity != null)
		{
			playerData.setProp("posX", controlledEntity.posX);
			playerData.setProp("posY", controlledEntity.posY);
			playerData.setProp("posZ", controlledEntity.posZ);
		}
		playerData.setProp("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerData.save();
		System.out.println("Player profile "+playerConnection.name+" saved.");
	}

	/*public void onJoin()
	{
		if(controlledEntity != null)
		{
			Server.getInstance().world.addEntity(controlledEntity);
			System.out.println("spawned player entity");
		}
	}*/
	
	public void destroy()
	{
		if(controlledEntity != null)
		{
			Server.getInstance().world.removeEntity(controlledEntity);
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
		return controlledEntity;
	}

	@Override
	public void setControlledEntity(Entity entity)
	{
		controlledEntity = entity;
		((EntityControllable) controlledEntity).setController(this);
		//Tells the player we assignated him an entity.
		if(controlledEntity != null)
		{
			Packet04Entity packet = new Packet04Entity(false);
			packet.defineControl = true;
			packet.includeName = true;
			packet.includeRotation = true;
			packet.applyFromEntity(controlledEntity);
			this.playerConnection.sendPacket(packet);
			this.hasSpawned = true;
			//System.out.println("hasSpawned = true");
		}
	}

	@Override
	public Location getPosition()
	{
		if(controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setPosition(Location l)
	{
		if(this.controlledEntity != null)
			this.controlledEntity.setPosition(l.x, l.y, l.z);
	}

	@Override
	public boolean isConnected()
	{
		return true;
	}

	@Override
	public void sendMessage(String msg)
	{
		playerConnection.sendChat(msg);
	}
	
	@Override
	public void kickPlayer(String reason)
	{
		Server.getInstance().handler.disconnectClient(playerConnection, reason);
	}

	public String toString()
	{
		return getName();
	}
	
	public String getDisplayName()
	{
		String name = getName();
		return ColorsTools.getUniqueColorPrefix(name)+name+"#FFFFFF";
	}

	@Override
	public <CE extends Entity & EntityControllable> void notifyTeleport(CE entity)
	{
		//Send teleport packet
		Packet04Entity packet = new Packet04Entity(false);
		packet.applyFromEntity(controlledEntity);
		/*packet.XBuffered = l.x;
		packet.YBuffered = l.y;
		packet.ZBuffered = l.z;*/
		System.out.println("!!!Sending packet with position "+entity);
		playerConnection.sendPacket(packet);
	}

	@Override
	public <CE extends Entity & EntityControllable> void notifyInventoryChange(CE entity)
	{
		System.out.println(this+" inventory changed sending packet");
		Packet05SerializedInventory packetInventory = new Packet05SerializedInventory(false);
		packetInventory.inventory = entity.inventory;
		playerConnection.sendPacket(packetInventory);
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		if (UsersPrivileges.isUserAdmin(getName()))
			return true;
		return false;
	}

	public Location getLastPosition()
	{
		if(this.playerData.isFieldSet("posX"))
		{
			return new Location(playerData.getDoubleProp("posX"), playerData.getDoubleProp("posY"), playerData.getDoubleProp("posZ"));
		}
		return null;
	}
}
