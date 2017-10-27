package io.xol.chunkstories.client;

import java.util.Iterator;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.ClientWorldLoadingAgent;
import io.xol.chunkstories.world.WorldClientCommon;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PlayerClientImplementation implements PlayerClient
{
	final Client client;
	final WorldClientCommon world;
	
	public final ClientWorldLoadingAgent loadingAgent;
	
	private EntityControllable controlledEntity;
	
	PlayerClientImplementation(Client client, WorldClientCommon world)
	{
		this.client = client;
		this.world = world;
		
		this.loadingAgent = new ClientWorldLoadingAgent(client, this, world);
	}

	@Override
	public ClientInputsManager getInputsManager()
	{
		return Client.getInstance().getInputsManager();
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
			
			//If a world master, directly set the entity's controller
			if(world instanceof WorldMaster)
				controllableEntity.getControllerComponent().setController(this);
		
			//In remote networked worlds, we need to subscribe the server to our player actions to the controlled entity so he gets updates
			if(entity.getWorld() instanceof WorldClientNetworkedRemote)
			{
				//When changing controlled entity, first unsubscribe the remote server from the one we no longer own
				if(controlledEntity != null && controllableEntity != controlledEntity)
					((WorldClientNetworkedRemote) controlledEntity.getWorld()).getRemoteServer().unsubscribe(controlledEntity);
				
				//Let know the server of new changes
				((WorldClientNetworkedRemote) controllableEntity.getWorld()).getRemoteServer().subscribe(controllableEntity);
			}
			
			controlledEntity = controllableEntity;
		}
		else if (entity == null && getControlledEntity() != null)
		{
			//Directly unset it
			if(world instanceof WorldMaster)
				getControlledEntity().getControllerComponent().setController(null);

			//When loosing control over an entity, stop sending the server info about it
			if(controlledEntity != null)
				if(controlledEntity.getWorld() instanceof WorldClientNetworkedRemote)
					((WorldClientNetworkedRemote) controlledEntity.getWorld()).getRemoteServer().unsubscribe(controlledEntity);
			
			controlledEntity = null;
		}
		
		return true;
	}

	@Override
	public SoundManager getSoundManager()
	{
		return Client.getInstance().getSoundManager();
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return world.getParticlesManager();
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return world.getDecalsManager();
	}

	@Override
	public long getUUID()
	{
		return Client.username.hashCode();
	}

	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unsubscribeAll()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushPacket(Packet packet)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSubscribedTo(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasFocus()
	{
		return client.hasFocus();
	}

	@Override
	public String getName()
	{
		return Client.username;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public void sendMessage(String msg)
	{
		client.printChat(msg);
	}

	@Override
	public Location getLocation()
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l)
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			controlledEntity.setLocation(l);
	}

	@Override
	public boolean isConnected()
	{
		return true;
	}

	@Override
	public boolean hasSpawned()
	{
		return controlledEntity != null;
	}

	@Override
	public void updateTrackedEntities()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public GameContext getContext()
	{
		return this.client;
	}

	@Override
	public World getWorld()
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			return controlledEntity.getWorld();
		return null;
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return true;
	}

	@Override
	public void flush()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect()
	{
		
	}

	@Override
	public void disconnect(String disconnectionReason)
	{
		
	}
	
	@Override
	public GameWindow getWindow()
	{
		return this.client.getGameWindow();
	}

	@Override
	public void openInventory(Inventory inventory)
	{
		Entity entity = this.getControlledEntity();
		if (inventory.isAccessibleTo(entity))
		{
			//Directly open it without further concern
			//Client.getInstance().openInventories(inventory);
			
			if(entity != null && entity instanceof EntityWithInventory)
				Client.getInstance().openInventories(((EntityWithInventory) entity).getInventory(), inventory);
			else
				Client.getInstance().openInventories(inventory);
		}
		//else
		//	this.sendMessage("Notice: You don't have access to this inventory.");
	}

	@Override
	public ClientInterface getClient() {
		return this.client;
	}
}
