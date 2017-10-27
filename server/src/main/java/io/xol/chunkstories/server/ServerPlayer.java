package io.xol.chunkstories.server;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponentInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.net.packets.PacketOpenInventory;
import io.xol.chunkstories.server.net.UserConnection;
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager;
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.sound.sources.VirtualSoundManager.ServerPlayerVirtualSoundManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerPlayer implements Player
{
	private ConfigFile playerDataFile;
	private UserConnection playerConnection;

	//Entity controlled
	private EntityControllable controlledEntity;

	//Streaming control
	private Set<Entity> subscribedEntities = ConcurrentHashMap.newKeySet();

	//Mirror of client inputs
	private ServerInputsManager serverInputsManager;

	//Dummy managers to relay synchronisation stuff
	private ServerPlayerVirtualSoundManager virtualSoundManager;
	private ServerPlayerVirtualParticlesManager virtualParticlesManager;
	private ServerPlayerVirtualDecalsManager virtualDecalsManager;

	public ServerPlayer(UserConnection playerConnection)
	{
		this.playerConnection = playerConnection;

		this.playerDataFile = new ConfigFile("./players/" + getPlayerConnection().name.toLowerCase() + ".cfg");

		this.serverInputsManager = new ServerInputsManager(this);

		//TODO this should be reset when the user changes world
		this.virtualSoundManager = playerConnection.getServer().getWorld().getSoundManager().new ServerPlayerVirtualSoundManager(this);
		this.virtualParticlesManager = playerConnection.getServer().getWorld().getParticlesManager().new ServerPlayerVirtualParticlesManager(this);
		this.virtualDecalsManager = playerConnection.getServer().getWorld().getDecalsManager().new ServerPlayerVirtualDecalsManager(this);

		// Sets dates
		this.playerDataFile.setString("lastlogin", "" + System.currentTimeMillis());
		if (this.playerDataFile.getString("firstlogin", "nope").equals("nope"))
			this.playerDataFile.setString("firstlogin", "" + System.currentTimeMillis());

		//Does not create a player entity here, this is taken care of by the player (re)spawn req
	}

	public UserConnection getPlayerConnection()
	{
		return playerConnection;
	}

	public void updateTrackedEntities()
	{
		//Checks ...
		if (controlledEntity == null)
			return;
		//Cache (idk if HotSpot makes it redudant but whatever)
		double ws = controlledEntity.getWorld().getWorldSize();
		Location controlledEntityLocation = controlledEntity.getLocation();
		Iterator<Entity> iter = controlledEntity.getWorld().getAllLoadedEntities();
		Entity e;
		boolean shouldTrack = false;
		//Let's iterate throught all of the world for now
		//TODO don't
		while (iter.hasNext())
		{
			e = iter.next();

			Location loc = e.getLocation();
			//Distance calculations
			double dx = LoopingMathHelper.moduloDistance(controlledEntityLocation.x(), loc.x(), ws);
			double dy = Math.abs(controlledEntityLocation.y() - loc.y());
			double dz = LoopingMathHelper.moduloDistance(controlledEntityLocation.z(), loc.z(), ws);
			shouldTrack = (dx < 256 && dz < 256 && dy < 256);
			boolean contains = subscribedEntities.contains(e) && e.shouldBeTrackedBy(this);
			//Too close and untracked
			if (shouldTrack && !contains)
			{
				this.subscribe(e);
			}
			//Too far but still tracked
			if (!shouldTrack && contains)
			{
				this.unsubscribe(e);

				subscribedEntities.remove(e); // Reminder, we are iterating the world, not trackedEntities
			}
		}

		Iterator<Entity> iter2 = subscribedEntities.iterator();
		while (iter2.hasNext())
		{
			e = iter2.next();

			Location loc = e.getLocation();
			//Distance calculations
			double dx = LoopingMathHelper.moduloDistance(controlledEntityLocation.x(), loc.x(), ws);
			double dy = Math.abs(controlledEntityLocation.y() - loc.y());
			double dz = LoopingMathHelper.moduloDistance(controlledEntityLocation.z(), loc.z(), ws);
			shouldTrack = (dx < 256 && dz < 256 && dy < 256);

			//Reasons other than distance to stop tracking this entity
			if (!e.shouldBeTrackedBy(this) || !shouldTrack)
			{
				this.unsubscribe(e);
			}
			else
			{
				// Just send new positions
				// ...
				// No need to do anything as the component system handles the updates
			}
		}
	}

	public void save()
	{
		long lastTime = playerDataFile.getLong("timeplayed", 0);
		long lastLogin = playerDataFile.getLong("lastlogin", 0);

		if (controlledEntity != null)
		{
			//Useless, kept for admin easyness, scripts, whatnot
			Location controlledEntityLocation = controlledEntity.getLocation();
			
			//Safely assumes as a SERVER the world will be master ;)
			WorldMaster world = (WorldMaster) controlledEntityLocation.getWorld();
			
			playerDataFile.setDouble("posX", controlledEntityLocation.x());
			playerDataFile.setDouble("posY", controlledEntityLocation.y());
			playerDataFile.setDouble("posZ", controlledEntityLocation.z());
			playerDataFile.setString("world", world.getWorldInfo().getInternalName());

			//Serializes the whole player entity !!!
			SerializedEntityFile playerEntityFile = new SerializedEntityFile(world.getFolderPath() + "/players/" + this.getName().toLowerCase() + ".csf");
			playerEntityFile.write(controlledEntity);
		}

		//Telemetry (EVIL)
		playerDataFile.setString("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerDataFile.save();

		System.out.println("Player profile " + getPlayerConnection().name + " saved.");
	}

	public void removePlayerFromWorld()
	{
		if (controlledEntity != null)
		{
			playerConnection.getServer().getWorld().removeEntity(controlledEntity);
		}
		unsubscribeAll();
	}

	@Override
	public String getName()
	{
		return getPlayerConnection().name;
	}

	@Override
	public EntityControllable getControlledEntity()
	{
		return controlledEntity;
	}

	@Override
	public boolean setControlledEntity(EntityControllable entity)
	{
		if (entity instanceof EntityControllable)
		{
			this.subscribe(entity);

			EntityControllable controllableEntity = (EntityControllable) entity;
			controllableEntity.getControllerComponent().setController(this);
			controlledEntity = controllableEntity;
		}
		else if (entity == null && getControlledEntity() != null)
		{
			getControlledEntity().getControllerComponent().setController(null);
			controlledEntity = null;
		}

		return true;
	}

	@Override
	public Location getLocation()
	{
		if (controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l)
	{
		if (this.controlledEntity != null)
			this.controlledEntity.setLocation(l);
	}

	/*@Override
	public void setFlying(boolean flying)
	{
		if (this.controlledEntity != null && this instanceof EntityFlying)
			((EntityFlying) this.controlledEntity).getFlyingComponent().set(flying);
	}*/

	@Override
	public boolean isConnected()
	{
		return playerConnection.isAlive();
	}

	@Override
	public void sendMessage(String msg)
	{
		getPlayerConnection().sendChat(msg);
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
		return ColorsTools.getUniqueColorPrefix(name) + name + "#FFFFFF";
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return DedicatedServer.server.getPermissionsManager().hasPermission(this, permissionNode);
	}

	public Location getLastPosition()
	{
		if (this.playerDataFile.isFieldSet("posX"))
		{
			return new Location(playerConnection.getServer().getWorld(), playerDataFile.getDouble("posX"), playerDataFile.getDouble("posY"), playerDataFile.getDouble("posZ"));
		}
		return null;
	}

	public boolean hasSpawned()
	{
		if (controlledEntity != null && controlledEntity.exists())
			return true;
		return false;
	}

	@Override
	public long getUUID()
	{
		//TODO make them proper
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
		if (subscribedEntities.add(entity))
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
		if (entity.unsubscribe(this))
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
		while (iterator.hasNext())
		{
			Entity entity = iterator.next();
			//If one of the entities is controllable ...
			if (entity instanceof EntityControllable)
			{
				EntityControllable controllableEntity = (EntityControllable) entity;
				Controller entityController = controllableEntity.getControllerComponent().getController();
				//If said entity is controlled by this subscriber/player
				if (entityController == this)
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
	public boolean isSubscribedTo(Entity entity)
	{
		return subscribedEntities.contains(entity);
	}

	// Managers

	@Override
	public InputsManager getInputsManager()
	{
		return serverInputsManager;
	}

	@Override
	public SoundManager getSoundManager()
	{
		return virtualSoundManager;
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return virtualParticlesManager;
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return virtualDecalsManager;
	}

	@Override
	public WorldServer getWorld()
	{
		if (controlledEntity != null)
			return (WorldServer) getLocation().getWorld();
		return null;
	}

	@Override
	public void pushPacket(Packet packet)
	{
		this.playerConnection.pushPacket(packet);
	}

	@Override
	public void flush()
	{
		this.playerConnection.flush();
	}

	@Override
	public void disconnect()
	{
		this.playerConnection.disconnect();
	}

	@Override
	public void disconnect(String disconnectionReason)
	{
		this.playerConnection.disconnect(disconnectionReason);
	}

	public ServerInterface getServer()
	{
		return playerConnection.getServer();
	}
	
	public ServerInterface getContext() {
		return getServer();
	}

	@Override
	public void openInventory(Inventory inventory)
	{
		Entity entity = this.getControlledEntity();
		if (inventory.isAccessibleTo(entity))
		{
			if (inventory instanceof EntityComponentInventory.EntityInventory)
			{
				//this.sendMessage("Notice: Pushing this inventory to you so you can see the contents");
				EntityComponentInventory.EntityInventory i = (EntityComponentInventory.EntityInventory)inventory;
				i.asComponent().pushComponent(this);
			}

			//this.sendMessage("Sending you the open inventory request.");
			PacketOpenInventory open = new PacketOpenInventory(inventory);
			this.pushPacket(open);
		}
		//else
		//	this.sendMessage("Notice: You don't have access to this inventory.");
	}
}
