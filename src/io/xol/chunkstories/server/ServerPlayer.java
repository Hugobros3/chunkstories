package io.xol.chunkstories.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.entity.EntityNameable;
import io.xol.chunkstories.entity.EntityRotateable;
import io.xol.chunkstories.net.packets.PacketEntity;
import io.xol.chunkstories.net.packets.PacketSerializedInventory;
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
	public EntityImplementation controlledEntity;

	//Streaming control
	public Map<int[], Long> loadedChunks = new HashMap<int[], Long>();
	public List<EntityImplementation> trackedEntities = new ArrayList<EntityImplementation>();
	
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
		Iterator<Entity> iter = controlledEntity.getWorld().entities.iterator();
		Entity e;
		double ws = controlledEntity.getWorld().getSizeSide();
		boolean shouldTrack = false;
		while(iter.hasNext())
		{
			e = iter.next();
			if(!e.equals(controlledEntity))
			{
				Location loc = e.getLocation();
				double dx = LoopingMathHelper.moduloDistance(controlledEntity.posX, loc.x, ws);
				double dy = Math.abs(controlledEntity.posY - loc.y);
				double dz = LoopingMathHelper.moduloDistance(controlledEntity.posZ, loc.z, ws);
				shouldTrack = (dx < 256 && dz < 256 && dy < 256);
				boolean contains = trackedEntities.contains(e);
				//System.out.println("[TRACKER] "+e+" shouldTrack:"+shouldTrack+" contains:"+contains+" "+this.playerConnection.name);
				if(shouldTrack && !contains)
				{
					trackedEntities.add((EntityImplementation) e);
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
		Iterator<EntityImplementation> iter2 = trackedEntities.iterator();
		while(iter.hasNext())
		{
			EntityImplementation e2 = iter2.next();
			if(e2.mpSendDeletePacket)
			{
				//Despawn the entity
				trackEntity(e2, false, true);
				iter.remove();
				//trackedEntities.remove(e);
			}
			else
			{
				// Just send new positions
				trackEntity(e2, false, false);
			}
		}
		//System.out.println("edguuuy");
	}
	
	public void trackEntity(Entity e, boolean first, boolean delete)
	{
		PacketEntity packet = new PacketEntity(false);
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
		controlledEntity = (EntityImplementation) entity;
		((EntityControllable) controlledEntity).setController(this);
		//Tells the player we assignated him an entity.
		if(controlledEntity != null)
		{
			PacketEntity packet = new PacketEntity(false);
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
	public void notifyTeleport(Entity entity)
	{
		//Send teleport packet
		PacketEntity packet = new PacketEntity(false);
		packet.applyFromEntity(controlledEntity);
		System.out.println("!!!Sending packet with position "+entity);
		playerConnection.sendPacket(packet);
	}

	@Override
	public void notifyInventoryChange(Entity entity)
	{
		System.out.println(this+" inventory changed sending packet");
		PacketSerializedInventory packetInventory = new PacketSerializedInventory(false);
		packetInventory.inventory = entity.getInventory();
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
			return new Location(Server.getInstance().world, playerData.getDoubleProp("posX"), playerData.getDoubleProp("posY"), playerData.getDoubleProp("posZ"));
		}
		return null;
	}
}
