package io.xol.chunkstories.server;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.misc.ConfigFile;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerPlayer implements Player
{
	ConfigFile playerDataFile;
	ServerClient playerConnection;

	//Entity controlled
	public EntityControllable controlledEntity;

	//Streaming control
	private Set<Entity> subscribedEntities = new HashSet<Entity>();

	//Mirror of client inputs
	private ServerInputsManager serverInputsManager;

	public ServerPlayer(ServerClient serverClient)
	{
		playerConnection = serverClient;
		
		playerDataFile = new ConfigFile("./players/" + playerConnection.name.toLowerCase() + ".cfg");
		
		serverInputsManager = new ServerInputsManager(this);
		
		// Sets dates
		playerDataFile.setString("lastlogin", "" + System.currentTimeMillis());
		if (playerDataFile.getProp("firstlogin", "nope").equals("nope"))
			playerDataFile.setString("firstlogin", "" + System.currentTimeMillis());
		//Does not create a player entity yet, this is taken care of by the player spawn req
	}

	public void updateTrackedEntities()
	{
		//Checks ...
		if(controlledEntity == null)
			return;
		//Cache (idk if HotSpot makes it redudant but whatever)
		double ws = controlledEntity.getWorld().getWorldSize();
		Location controlledEntityLocation = controlledEntity.getLocation();
		Iterator<Entity> iter = controlledEntity.getWorld().getAllLoadedEntities();
		Entity e;
		boolean shouldTrack = false;
		//Let's iterate throught all of the world for now
		//TODO don't
		
		while(iter.hasNext())
		{
			e = iter.next();
			
			//Don't track ourselves
			//if(!e.equals(controlledEntity))
			{
				Location loc = e.getLocation();
				//Distance calculations
				double dx = LoopingMathHelper.moduloDistance(controlledEntityLocation.x, loc.x, ws);
				double dy = Math.abs(controlledEntityLocation.y - loc.y);
				double dz = LoopingMathHelper.moduloDistance(controlledEntityLocation.z, loc.z, ws);
				shouldTrack = (dx < 256 && dz < 256 && dy < 256);
				boolean contains = subscribedEntities.contains(e) && e.shouldBeTrackedBy(this);
				//Too close and untracked
				if(shouldTrack && !contains)
				{
					this.subscribe(e);
					//trackEntity(e, true, false);
				}
				//Too far but still tracked
				if(!shouldTrack && contains)
				{
					//Despawn the entity
					System.out.println("Unsubscribed "+this+" from "+e+" because of distance");
					
					this.unsubscribe(e);
					//trackEntity(e, false, true);
					
					subscribedEntities.remove(e); // Reminder, we are iterating the world, not trackedEntities
				}
			}
		}
		Iterator<Entity> iter2 = subscribedEntities.iterator();
		while(iter.hasNext())
		{
			e = iter2.next();
			//Reasons other than distance to stop tracking this entity
			if(!e.shouldBeTrackedBy(this))
			{
				//Despawn the entity
				//trackEntity(e, false, true);
				System.out.println("Unsubscribed "+this+" from "+e+" because of IM A MORON");
				this.unsubscribe(e);
				iter.remove();
			}
			else
			{
				// Just send new positions
				
				//trackEntity(e, false, false);
				//No need to do anything as the component system handles the updates
			}
		}
	}

	public void save()
	{
		long lastTime = Long.parseLong(playerDataFile.getProp("timeplayed", "0"));
		long lastLogin = Long.parseLong(playerDataFile.getProp("lastlogin", "0"));
		
		if(controlledEntity != null)
		{
			//Useless, kept for admin easyness, scripts, whatnot
			Location controlledEntityLocation = controlledEntity.getLocation();
			playerDataFile.setDouble("posX", controlledEntityLocation.x);
			playerDataFile.setDouble("posY", controlledEntityLocation.y);
			playerDataFile.setDouble("posZ", controlledEntityLocation.z);
			
			//Serializes the whole player entity !!!
			SerializedEntityFile playerEntityFile = new SerializedEntityFile("./players/" + this.getName().toLowerCase() + ".csf");
			playerEntityFile.write(controlledEntity);
		}
		
		//Telemetry (EVIL)
		playerDataFile.setString("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerDataFile.save();
		
		System.out.println("Player profile "+playerConnection.name+" saved.");
	}
	
	public void destroy()
	{
		if(controlledEntity != null)
		{
			Server.getInstance().getWorld().removeEntity(controlledEntity);
			System.out.println("removed player entity");
		}
		unsubscribeAll();
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
		if(entity instanceof EntityControllable)
		{
			this.subscribe(entity);
			
			EntityControllable controllableEntity = (EntityControllable)entity;
			controllableEntity.getControllerComponent().setController(this);
			controlledEntity = controllableEntity;
		}
	}

	@Override
	public Location getLocation()
	{
		if(controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l)
	{
		if(this.controlledEntity != null)
			this.controlledEntity.setLocation(l);
	}
	
	@Override
	public void setFlying(boolean flying)
	{
		if(this.controlledEntity != null && this instanceof EntityFlying)
			((EntityFlying) this.controlledEntity).getFlyingComponent().setFlying(flying);
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
		playerConnection.disconnect("Kicked : "+reason);
	}

	@Override
	public String toString()
	{
		return getName();
	}
	
	@Override
	public String getDisplayName()
	{
		String name = getName();
		//Hashed username for colour :)
		return ColorsTools.getUniqueColorPrefix(name)+name+"#FFFFFF";
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
		if(this.playerDataFile.isFieldSet("posX"))
		{
			return new Location(Server.getInstance().getWorld(), playerDataFile.getDouble("posX"), playerDataFile.getDouble("posY"), playerDataFile.getDouble("posZ"));
		}
		return null;
	}

	public boolean hasSpawned()
	{
		if(controlledEntity != null && controlledEntity.exists())
			return true;
		return false;
	}

	@Override
	public long getUUID()
	{
		return this.getName().hashCode();
	}

	// Entity tracking
	
	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		return subscribedEntities.iterator();
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		if(subscribedEntities.add(entity))
		{
			entity.subscribe(this);
			
			//Only the server should ever push all components to a client
			entity.getComponents().pushAllComponents(this);
			return true;
		}
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		if(entity.unsubscribe(this))
		{
			subscribedEntities.remove(entity);
			return true;
		}
		return false;
	}

	@Override
	public void unsubscribeAll()
	{
		Iterator<Entity> iterator = getSubscribedToList();
		while(iterator.hasNext())
		{
			Entity entity = iterator.next();
			//If one of the entities is controllable ...
			if(entity instanceof EntityControllable)
			{
				EntityControllable controllableEntity = (EntityControllable)entity;
				Controller entityController = controllableEntity.getControllerComponent().getController();
				//If said entity is controlled by this subscriber/player
				if(entityController == this)
				{
					//Set the controller to null
					controllableEntity.getControllerComponent().setController(null);
				}
			}
			entity.unsubscribe(this);
			iterator.remove();
		}
	}

	@Override
	public void pushPacket(Packet packet)
	{
		this.playerConnection.pushPacket(packet);
	}

	@Override
	public boolean isSubscribedTo(Entity entity)
	{
		return subscribedEntities.contains(entity);
	}

	@Override
	public InputsManager getInputsManager()
	{
		return serverInputsManager;
	}
}
