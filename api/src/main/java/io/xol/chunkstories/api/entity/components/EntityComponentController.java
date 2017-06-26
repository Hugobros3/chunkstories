package io.xol.chunkstories.api.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.exceptions.UnauthorizedClientActionException;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentController extends EntityComponent
{
	public EntityComponentController(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	Controller controller = null;

	public Controller getController()
	{
		return controller;
	}

	public void setController(Controller controller)
	{
		//Checks we are entitled to do this
		if (!(entity.getWorld() instanceof WorldMaster))
			throw new UnauthorizedClientActionException("setController()");

		Controller formerController = this.controller;
		this.controller = controller;
		//Tell the new controller he his
		if (controller != null)
			pushComponent(controller);
		//Tell the former one he's no longer
		if (formerController != null && (controller == null || !controller.equals(formerController)))
			pushComponent(formerController);
	}

	@Override
	public void push(StreamTarget to, DataOutputStream dos) throws IOException
	{
		//We write if the controller exists and if so we tell the uuid
		dos.writeBoolean(controller != null);
		if (controller != null)
			dos.writeLong(controller.getUUID());
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		long controllerUUID = 0;
		boolean isControllerNotNull = dis.readBoolean();
		if(isControllerNotNull)
			controllerUUID = dis.readLong();
		
		//This is strictly illegal, only remotely connected client worlds will accept such requests
		if (!(entity.getWorld() instanceof WorldClientNetworkedRemote))
		{
			//Terminate connections immediately
			if(from instanceof Player)
				((Player)from).disconnect("Illegal controller set attempt, terminating client connection for "+from);
			return;
		}
		
		PlayerClient player = ((ClientInterface)entity.getWorld().getGameContext()).getPlayer();
		assert player != null;
		
		if (isControllerNotNull)
		{
			//long controllerUUID = dis.readLong();
				
			long clientUUID = player.getUUID();
			System.out.println("Entity " + entity + " is now in control of " + controllerUUID + " me=" + clientUUID);
			
			//This update tells us we are now in control of this entity
			if (clientUUID == controllerUUID)
			{
				EntityControllable controlledEntity = (EntityControllable) entity;
				
				//TODO sort out local hosted worlds properly ?
				
				//Client.getInstance().getServerConnection().subscribe(entity);
				controller = player;

				player.setControlledEntity(controlledEntity);
				System.out.println("The client is now in control of entity "+controlledEntity);
			}
			else
			{
				//If we receive a different UUID than ours in a EntityComponent change, it means that we don't control it anymore and someone else does.
				if (player.getControlledEntity() != null && player.getControlledEntity().equals(entity))
				{
					player.setControlledEntity(null);

					//Client.getInstance().getServerConnection().unsubscribe(entity);
					controller = null;
					System.out.println("Lost control of entity " + entity + " to " + controllerUUID);
				}
			}
			
		}
		else
		{

			//If we are a client.
			
				//If we receive a different UUID than ours in a EntityComponent change, it means that we don't control it anymore and someone else does.
				if (player.getControlledEntity() != null && player.getControlledEntity().equals(entity))
				{
					player.setControlledEntity(null);

					//Client.getInstance().getServerConnection().unsubscribe(entity);
					controller = null;
					System.out.println("Lost control of entity " + entity);
				}
			
		}
	}

}
