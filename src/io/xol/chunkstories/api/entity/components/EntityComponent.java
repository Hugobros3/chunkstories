package io.xol.chunkstories.api.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityControllable;
import io.xol.chunkstories.api.net.StreamTarget;
import io.xol.chunkstories.entity.EntityComponents;
import io.xol.chunkstories.net.packets.PacketEntity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityComponent
{
	protected Entity entity;
	EntityComponent next;
	
	public EntityComponent(Entity entity, EntityComponent previous)
	{
		this.entity = entity;
		if(previous != null)
			previous.setNext(this);
	}
	
	public void setNext(EntityComponent next)
	{
		this.next = next;
	}
	
	public EntityComponent getLastComponent()
	{
		if(next != null)
			return next.getLastComponent();
		else return this;
	}
	
	/**
	 * Push will tell all subscribers of the entity about a change of this component only
	 */
	public void pushComponentEveryone()
	{
		Iterator<Subscriber> iterator = entity.getAllSubscribers();
		while(iterator.hasNext())
		{
			Subscriber subscriber = iterator.next();
			
			PacketEntity packet = new PacketEntity(subscriber.getUUID() == -1);
			packet.entityUUID = entity.getUUID();
			packet.entityTypeID = entity.getEID();
			
			packet.updateOneComponent = this;
			subscriber.pushPacket(packet);
		}
	}
	
	public void pushComponentEveryoneButController()
	{
		//System.out.println("pushing2all");
		Iterator<Subscriber> iterator = entity.getAllSubscribers();
		
		Controller controller = null;
		if(entity instanceof EntityControllable)
		{
			controller = ((EntityControllable) entity).getControllerComponent().getController();
		}
		while(iterator.hasNext())
		{
			Subscriber subscriber = iterator.next();
			
			//System.out.println("pushing2"+subscriber);
			
			//Don't push the update to the controller.
			if(controller != null && subscriber.equals(controller))
				continue;
			
			PacketEntity packet = new PacketEntity(subscriber.getUUID() == -1);
			packet.entityUUID = entity.getUUID();
			packet.entityTypeID = entity.getEID();
			
			packet.updateOneComponent = this;
			subscriber.pushPacket(packet);
		}
	}
	
	public void pushComponent(Subscriber subscriber)
	{
		//TODO check that subscriber has subscribed to said entity ?
		//Re : nope because we send the EntityExistence (hint: false) component to [just] unsubscribed guys
		
		PacketEntity packet = new PacketEntity(subscriber.getUUID() == -1);
		packet.entityUUID = entity.getUUID();
		packet.entityTypeID = entity.getEID();
		
		//Set the packet to "just update that component" mode
		packet.updateOneComponent = this;
		subscriber.pushPacket(packet);
	}
	
	public void pushComponentInStream(StreamTarget to, DataOutputStream dos) throws IOException
	{
		dos.writeInt(getEntityComponentId());
		push(to, dos);
	}
	
	public void pushAllComponentsInStream(StreamTarget to, DataOutputStream dos) throws IOException
	{
		pushComponentInStream(to, dos);
		if(next != null)
			next.pushAllComponentsInStream(to, dos);
	}

	public void pushAllComponents(Subscriber subscriber)
	{
		PacketEntity packet = new PacketEntity(subscriber.getUUID() == -1);
		packet.entityUUID = entity.getUUID();
		packet.entityTypeID = entity.getEID();
		
		//Set the packet to "update everything in the chained list" mode
		packet.updateManyComponents = this;
		subscriber.pushPacket(packet);
	}
	
	public boolean tryPull(int componentId, DataInputStream dis) throws IOException
	{
		//Does the Id match ?
		if(this.getEntityComponentId() == componentId)
		{
			pull(dis);
			return true;
		}
		//Chain next component
		if(next != null)
			return next.tryPull(componentId, dis);
		return false;
	}

	protected abstract void push(StreamTarget destinator, DataOutputStream dos) throws IOException;
	
	protected abstract void pull(DataInputStream dis) throws IOException;

	public int getEntityComponentId(){
		//System.out.println("debug : "+this.getClass().getName()+" id = "+EntityComponents.getIdForClass(this.getClass().getName()));
		return EntityComponents.getIdForClass(this.getClass().getName());
	}
}
