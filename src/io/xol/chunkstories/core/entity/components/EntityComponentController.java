package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.exceptions.UnauthorizedClientActionException;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;

//(c) 2015-2016 XolioWare Interactive
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
		boolean isControllerNotNull = dis.readBoolean();
		if (isControllerNotNull)
		{
			long controllerUUID = dis.readLong();
			//If we are a client.
			if (entity.getWorld() instanceof WorldClient)
			{
				long clientUUID = Client.getInstance().getUUID();
				System.out.println("Entity " + entity + " is now in control of " + controllerUUID + " me=" + clientUUID);
				if (clientUUID == controllerUUID)
				{
					//This update tells us we are now in control of this entity
					EntityControllable controlledEntity = (EntityControllable) entity;
					Client.getInstance().getServerConnection().subscribe(entity);
					controller = Client.getInstance();

					Client.getInstance().setControlledEntity(controlledEntity);
					System.out.println("controlledEntity lel");
				}
				else
				{
					//If we receive a different UUID than ours in a EntityComponent change, it means that we don't control it anymore and someone else does.
					if (Client.getInstance().getControlledEntity() != null && Client.getInstance().getControlledEntity().equals(entity))
					{
						Client.getInstance().setControlledEntity(null);

						//Client.getInstance().getServerConnection().unsubscribe(entity);
						controller = null;
						System.out.println("Lost control of entity " + entity + " to " + controllerUUID);
					}
				}
			}
		}
		else
		{

			//If we are a client.
			if (entity.getWorld() instanceof WorldClient)
			{
				//If we receive a different UUID than ours in a EntityComponent change, it means that we don't control it anymore and someone else does.
				if (Client.getInstance().getControlledEntity() != null && Client.getInstance().getControlledEntity().equals(entity))
				{
					Client.getInstance().setControlledEntity(null);

					//Client.getInstance().getServerConnection().unsubscribe(entity);
					controller = null;
					System.out.println("Lost control of entity " + entity);
				}
			}
		}
	}

}
