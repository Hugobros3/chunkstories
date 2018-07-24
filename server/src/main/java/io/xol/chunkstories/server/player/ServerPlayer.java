//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.player;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3d;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.serializable.TraitController;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.entity.traits.serializable.TraitSerializable;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.packets.PacketOpenInventory;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.server.RemotePlayer;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.server.ServerInputsManager;
import io.xol.chunkstories.server.net.ClientConnection;
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager;
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager;
import io.xol.chunkstories.sound.VirtualSoundManager.ServerPlayerVirtualSoundManager;
import io.xol.chunkstories.util.config.OldStyleConfigFile;
import io.xol.chunkstories.world.WorldServer;

public class ServerPlayer implements RemotePlayer {
	protected final ClientConnection connection;
	protected final String name;
	protected final ServerInterface server;

	private OldStyleConfigFile playerDataFile;

	private WorldServer world;
	private Entity controlledEntity;

	// Streaming control
	private Set<Entity> subscribedEntities = ConcurrentHashMap.newKeySet();

	// Mirror of client inputs
	private ServerInputsManager serverInputsManager;

	// Dummy managers to relay synchronisation stuff
	private ServerPlayerVirtualSoundManager virtualSoundManager;
	private ServerPlayerVirtualParticlesManager virtualParticlesManager;
	private ServerPlayerVirtualDecalsManager virtualDecalsManager;

	public final RemotePlayerLoadingAgent loadingAgent = new RemotePlayerLoadingAgent(this);

	public ServerPlayer(ClientConnection playerConnection, String name) {
		this.connection = playerConnection;
		this.name = name;

		this.server = playerConnection.getContext();

		this.playerDataFile = new OldStyleConfigFile("./players/" + name.toLowerCase() + ".cfg");

		this.serverInputsManager = new ServerInputsManager(this);

		// Sets dates
		this.playerDataFile.setString("lastlogin", "" + System.currentTimeMillis());
		if (this.playerDataFile.getString("firstlogin", "nope").equals("nope"))
			this.playerDataFile.setString("firstlogin", "" + System.currentTimeMillis());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getUUID() {
		// TODO make them longer
		return this.getName().hashCode();
	}

	@Override
	public String getDisplayName() {
		String name = getName();
		// Hashed username for colour :)
		return ColorsTools.getUniqueColorPrefix(name) + name + "#FFFFFF";
	}

	/** Asks the server's permission manager if the player is ok to do that */
	@Override
	public boolean hasPermission(String permissionNode) {
		return connection.getContext().getPermissionsManager().hasPermission(this, permissionNode);
	}

	@Override
	public WorldServer getWorld() {
		// if (controlledEntity != null)
		// return (WorldServer) getLocation().getWorld();
		return world;
	}

	public void setWorld(WorldServer world) {
		this.world = world;
		
		this.virtualSoundManager = world.getSoundManager().new ServerPlayerVirtualSoundManager(this);
		this.virtualParticlesManager = world.getParticlesManager().new ServerPlayerVirtualParticlesManager(this);
		this.virtualDecalsManager = world.getDecalsManager().new ServerPlayerVirtualDecalsManager(this);
	}

	public boolean hasSpawned() {
		if (controlledEntity != null && !controlledEntity.entityLocation.wasRemoved())
			return true;
		return false;
	}

	@Override
	public Location getLocation() {
		if (controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l) {
		if (this.controlledEntity != null)
			this.controlledEntity.entityLocation.set(l);
	}

	public Location getLastPosition() {
		if (this.playerDataFile.isFieldSet("posX")) {
			return new Location(getWorld(), playerDataFile.getDouble("posX"), playerDataFile.getDouble("posY"),
					playerDataFile.getDouble("posZ"));
		}
		return null;
	}

	public void removeEntityFromWorld() {
		if (controlledEntity != null) {
			getWorld().removeEntity(controlledEntity);
		}
		unsubscribeAll();
	}

	// Entity control
	@Override
	public Entity getControlledEntity() {
		return controlledEntity;
	}

	@Override
	public boolean setControlledEntity(Entity entity) {
		//TODO lock for safety
		TraitController ec = entity != null ? entity.traits.get(TraitController.class) : null;
		if (entity != null && ec != null) {
			this.subscribe(entity);

			ec.setController(this);
			controlledEntity = entity;
		} else if (entity == null && controlledEntity != null) {
			controlledEntity.traits.with(TraitController.class, ec2 -> ec2.setController(null));
			controlledEntity = null;
		}

		return true;
	}

	@Override
	public void openInventory(Inventory inventory) {
		Entity entity = this.getControlledEntity();
		if (inventory.isAccessibleTo(entity)) {
			if (inventory instanceof TraitInventory) {
				TraitInventory i = (TraitInventory) inventory;
				i.pushComponent(this);
			}

			// this.sendMessage("Sending you the open inventory request.");
			PacketOpenInventory open = new PacketOpenInventory(getWorld(), inventory);
			this.pushPacket(open);
		}
		// else
		// this.sendMessage("Notice: You don't have access to this inventory.");
	}

	// Entity tracking
	public void updateTrackedEntities() {
		Entity controlledEntity = this.controlledEntity;
		if (controlledEntity == null)
			return;

		// Cache (idk if HotSpot makes it redudant but whatever)
		double world_size = controlledEntity.getWorld().getWorldSize();
		Location controlledTraitLocation = controlledEntity.getLocation();

		double ENTITY_VISIBILITY_SIZE = 192;

		Iterator<Entity> inRangeEntitiesIterator = controlledEntity.getWorld().getEntitiesInBox(
				controlledTraitLocation,
				new Vector3d(ENTITY_VISIBILITY_SIZE, ENTITY_VISIBILITY_SIZE, ENTITY_VISIBILITY_SIZE));
		while (inRangeEntitiesIterator.hasNext()) {
			Entity e = inRangeEntitiesIterator.next();

			boolean shouldTrack = true;//e.shouldBeTrackedBy(this);
			boolean contains = subscribedEntities.contains(e);

			if (shouldTrack && !contains)
				this.subscribe(e);

			if (!shouldTrack && contains)
				this.unsubscribe(e);
		}

		Iterator<Entity> subscribedEntitiesIterator = subscribedEntities.iterator();
		while (subscribedEntitiesIterator.hasNext()) {
			Entity e = subscribedEntitiesIterator.next();

			Location loc = e.getLocation();

			// Distance calculations
			double dx = LoopingMathHelper.moduloDistance(controlledTraitLocation.x(), loc.x(), world_size);
			double dy = Math.abs(controlledTraitLocation.y() - loc.y());
			double dz = LoopingMathHelper.moduloDistance(controlledTraitLocation.z(), loc.z(), world_size);
			boolean inRange = (dx < ENTITY_VISIBILITY_SIZE && dz < ENTITY_VISIBILITY_SIZE
					&& dy < ENTITY_VISIBILITY_SIZE);

			// System.out.println(inRange);

			// Reasons other than distance to stop tracking this entity
			if (/*!e.shouldBeTrackedBy(this) || */!inRange)
				this.unsubscribe(e);

			// No need to do anything as the component system handles the updates
		}
	}

	@Override
	public Iterator<Entity> getSubscribedToList() {
		return subscribedEntities.iterator();
	}

	@Override
	public boolean subscribe(Entity entity) {
		if (subscribedEntities.add(entity)) {
			entity.subscribers.register(this);

			// Only the server should ever push all components to a client
			entity.traits.all().forEach(c -> {if(c instanceof TraitSerializable) ((TraitSerializable) c).pushComponent(this); });
			return true;
		}
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity) {
		// Thread.dumpStack();
		// System.out.println("sub4sub");
		if (entity.subscribers.unregister(this)) // TODO REMOVE ENTITY EXISTENCE COMPONENT IT'S STUPID AND WRONG
		{
			subscribedEntities.remove(entity);
			return true;
		}
		return false;
	}

	@Override
	public void unsubscribeAll() {
		Iterator<Entity> iterator = getSubscribedToList();
		while (iterator.hasNext()) {
			Entity entity = iterator.next();
			// If one of the entities is controllable ...
			entity.traits.with(TraitController.class, ec -> {
				Controller TraitController = ec.getController();
				// If said entity is controlled by this subscriber/player
				if (TraitController == this) {
					// Set the controller to null
					ec.setController(null);
				}
			});
			
			entity.subscribers.unregister(this);
			iterator.remove();
		}
	}

	public boolean isSubscribedTo(Entity entity) {
		return subscribedEntities.contains(entity);
	}

	// Various subsystems managers
	@Override
	public InputsManager getInputsManager() {
		return serverInputsManager;
	}

	@Override
	public SoundManager getSoundManager() {
		return virtualSoundManager;
	}

	@Override
	public ParticlesManager getParticlesManager() {
		return virtualParticlesManager;
	}

	@Override
	public DecalsManager getDecalsManager() {
		return virtualDecalsManager;
	}

	@Override
	public void sendMessage(String message) {
		getPlayerConnection().sendTextMessage("chat/" + message);
	}

	@Override
	public void pushPacket(Packet packet) {
		this.connection.pushPacket(packet);
	}

	@Override
	public void flush() {
		this.connection.flush();
	}

	@Override
	public void disconnect() {
		this.connection.disconnect();
	}

	@Override
	public void disconnect(String disconnectionReason) {
		this.connection.disconnect(disconnectionReason);
	}

	@Override
	public boolean isConnected() {
		return connection.isOpen();
	}

	public ClientConnection getPlayerConnection() {
		return connection;
	}

	public ServerInterface getContext() {
		return server;
	}

	/** Serializes the stuff describing this player */
	public void save() {
		long lastTime = playerDataFile.getLong("timeplayed", 0);
		long lastLogin = playerDataFile.getLong("lastlogin", 0);

		if (controlledEntity != null) {
			// Useless, kept for admin easyness, scripts, whatnot
			Location controlledTraitLocation = controlledEntity.getLocation();

			// Safely assumes as a SERVER the world will be master ;)
			WorldMaster world = (WorldMaster) controlledTraitLocation.getWorld();

			playerDataFile.setDouble("posX", controlledTraitLocation.x());
			playerDataFile.setDouble("posY", controlledTraitLocation.y());
			playerDataFile.setDouble("posZ", controlledTraitLocation.z());
			playerDataFile.setString("world", world.getWorldInfo().getInternalName());

			// Serializes the whole player entity !!!
			SerializedEntityFile playerEntityFile = new SerializedEntityFile(
					world.getFolderPath() + "/players/" + this.getName().toLowerCase() + ".csf");
			playerEntityFile.write(controlledEntity);
		}

		// Telemetry (EVIL)
		playerDataFile.setString("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerDataFile.save();

		System.out.println("Player profile " + name + " saved.");
	}

	@Override
	public String toString() {
		return getName();
	}

	public void destroy() {
		save();
		removeEntityFromWorld();

		loadingAgent.destroy();
	}
}
