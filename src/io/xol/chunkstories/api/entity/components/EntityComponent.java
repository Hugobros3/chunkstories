package io.xol.chunkstories.api.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.serialization.OfflineSerializedData;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.net.packets.PacketEntity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * To make a new entity component, just extend this class and mention your class a .components file in the entity/ directory so it gets an unique id
 */
public abstract class EntityComponent
{
	protected Entity entity;
	EntityComponent next;
	
	private final int ecID;

	public EntityComponent(Entity entity)
	{
		this(entity, entity.getComponents().getLastComponent());
	}

	public EntityComponent(Entity entity, EntityComponent previous)
	{
		this.ecID = previous != null ? previous.getEntityComponentId() + 1 : 1;
		//this.ecID = (entity == null || entity.getWorld() == null ) ? -1 : entity.getWorld().getGameContext().getContent().entities().components().getIdForClass(getClass().getName());
		
		this.entity = entity;
		if (previous != null)
			previous.setNext(this);
	}

	public void setNext(EntityComponent next)
	{
		this.next = next;
	}

	public EntityComponent getLastComponent()
	{
		if (next != null)
			return next.getLastComponent();
		else
			return this;
	}

	/**
	 * Push will tell all subscribers of the entity about a change of this component only
	 */
	public void pushComponentEveryone()
	{
		Iterator<Subscriber> iterator = entity.getAllSubscribers();
		while (iterator.hasNext())
		{
			Subscriber subscriber = iterator.next();

			try
			{
				PacketEntity packet = new PacketEntity(entity);
				this.pushComponentInStream(subscriber, packet.getSynchPacketOutputStream());
				subscriber.pushPacket(packet);
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Push the component to the controller, if such one exists
	 */
	public void pushComponentController()
	{
		if (this.entity instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this.entity).getControllerComponent().getController();
			if (controller != null)
			{
				pushComponent(controller);
			}
		}
	}

	/**
	 * Push the component to everyone but the controller, if such one exists
	 */
	public void pushComponentEveryoneButController()
	{
		//System.out.println("pushing2all");
		Iterator<Subscriber> iterator = entity.getAllSubscribers();

		Controller controller = null;
		if (entity instanceof EntityControllable)
		{
			controller = ((EntityControllable) entity).getControllerComponent().getController();
		}
		while (iterator.hasNext())
		{
			Subscriber subscriber = iterator.next();

			//Don't push the update to the controller.
			if (controller != null && subscriber.equals(controller))
				continue;

			try
			{
				PacketEntity packet = new PacketEntity(entity);
				this.pushComponentInStream(subscriber, packet.getSynchPacketOutputStream());
				subscriber.pushPacket(packet);
			}
			catch (IOException e)
			{
			}
		}
	}

	public void pushComponent(Subscriber subscriber)
	{
		//You may check that subscriber has subscribed to said entity ?
		//A: nope because we send the EntityExistence (hint: false) component to [just] unsubscribed guys so it wouldn't work
		try
		{
			PacketEntity packet = new PacketEntity(entity);
			this.pushComponentInStream(subscriber, packet.getSynchPacketOutputStream());
			subscriber.pushPacket(packet);
		}
		catch (IOException e)
		{
		}
	}

	public void pushComponentInStream(StreamTarget to, DataOutputStream dos) throws IOException
	{
		//Offline saves will have version discrepancies, so we use a symbolic name instead
		if(to instanceof OfflineSerializedData)
		{
			dos.writeInt(-1);
			dos.writeUTF(getSerializedComponentName());
		}
		else
		{
			dos.writeInt(getEntityComponentId());
		}
		
		//System.out.println("Pushing "+this.getSerializedComponentName()+" to "+to);
		
		//Push actual component data
		push(to, dos);
	}

	public final void pushAllComponentsInStream(StreamTarget to, DataOutputStream dos) throws IOException
	{
		pushComponentInStream(to, dos);
		if (next != null)
			next.pushAllComponentsInStream(to, dos);
	}

	public final void pushAllComponents(Subscriber subscriber)
	{
		try
		{
			PacketEntity packet = new PacketEntity(entity);
			this.pushAllComponentsInStream(subscriber, packet.getSynchPacketOutputStream());
			subscriber.pushPacket(packet);
		}
		catch (IOException e)
		{
			
		}
	}

	public final void tryPullComponentInStream(int componentId, StreamSource from, DataInputStream dis) throws IOException, UnknownComponentException
	{
		//Does the Id match ?
		if (this.getEntityComponentId() == componentId)
		{
			//System.out.println("Pulling "+this.getSerializedComponentName()+" from "+from);
			pull(from, dis);
			return;
		}
		//Chain next component
		if (next != null)
			next.tryPullComponentInStream(componentId, from, dis);
		else
			throw new UnknownComponentException(componentId, entity.getClass());
	}
	
	public final void tryPullComponentInStream(String componentName, StreamSource from, DataInputStream dis) throws IOException, UnknownComponentException
	{
		//Does the Id match ?
		if (this.getSerializedComponentName().equals(componentName))
		{
			pull(from, dis);
			return;
		}
		//Chain next component
		if (next != null)
			next.tryPullComponentInStream(componentName, from, dis);
		else
			throw new UnknownComponentException(componentName, entity.getClass());
	}

	protected abstract void push(StreamTarget destinator, DataOutputStream dos) throws IOException;

	protected abstract void pull(StreamSource from, DataInputStream dis) throws IOException;

	public final int getEntityComponentId()
	{
		return ecID;
	}
	
	public String getSerializedComponentName()
	{
		return this.getClass().getSimpleName();
	}

	public EntityComponent getComponentById(short componentId)
	{
		if(ecID == componentId)
			return this;
		
		if(next != null)
			return next.getComponentById(componentId);
		
		return null;
	}
}
