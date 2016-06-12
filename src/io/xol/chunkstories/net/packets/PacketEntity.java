package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.entity.EntityImplementation;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketEntity extends Packet
{
	/**
	 * Transfers essential data about entities
	 */
	//private Entity entity;
	public short entityTypeID;
	public long entityUUID;
	
	public EntityComponent updateOneComponent;
	public EntityComponent updateManyComponents;

	//public boolean deleteFlag = false; // Tells client to stop tracking this entity and delete it
	
	//World world;

	/*public double XBuffered, YBuffered, ZBuffered;
	public double RHBuffered, RVBuffered;
	public String nBuffered;

	public boolean defineControl = false; // Tells the client that the player entity is this one.
	public boolean includeRotation = false; // Tells both sides to consider extra 2 doubles for yaw and pitch
	public boolean includeName = false; // This is a nameable entity
	public boolean deleteFlag = false; // Tells client to stop tracking this entity and delete it
	public boolean includeVelocity = false; // Include or not position interpolation

	private byte[] csfData;
	*/
	//private Entity entity;
	
	public PacketEntity(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//System.out.println("Sending entity " + entityID + " EID : " + entityType + " PosX" + XBuffered + (nBuffered == null ? "null" : nBuffered));
		out.writeLong(entityUUID);
		out.writeShort(entityTypeID);
		
		//Write all components we wanna update
		if(updateOneComponent != null)
		{
			updateOneComponent.pushComponentInStream(destinator, out);
		}
		else
			updateManyComponents.pushAllComponentsInStream(destinator, out);
		
		//Then write 0
		out.writeInt((int)0);
	}

	public void process(DataInputStream in, PacketsProcessor processor) throws IOException, UnknownComponentException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();
		
		Entity entity = processor.getWorld().getEntityByUUID(this.entityUUID);
		
		boolean addToWorld = false;
		//Create an entity if the servers tells you to do so
		if(entity == null)
		{
			entity = EntitiesList.newEntity(processor.getWorld(), this.entityTypeID);
			entity.setUUID(entityUUID);
			
			addToWorld = true;
		}
		
		int componentId = in.readInt();
		//Loop throught all components
		while(componentId != 0)
		{
			//System.out.println("got component : "+componentId);
			if(!entity.getComponents().tryPull(componentId, in))
				throw new UnknownComponentException(componentId, entity.getClass());
			componentId = in.readInt();
		}
		
		if(addToWorld)
		{
			//Only the WorldMaster is allowed to spawn new entities in the world
			if(processor.isClient)
				processor.getWorld().addEntity(entity);
		}
	}
	
	private void read(DataInputStream in) throws IOException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();

		/*XBuffered = in.readDouble();
		YBuffered = in.readDouble();
		ZBuffered = in.readDouble();

		byte byteField = in.readByte();
		defineControl = ((byteField >> 0) & 0x01) == 1;
		includeRotation = ((byteField >> 1) & 0x01) == 1;
		includeName = ((byteField >> 2) & 0x01) == 1;
		deleteFlag = ((byteField >> 3) & 0x01) == 1;

		if (includeRotation)
		{
			RHBuffered = (float) in.readDouble();
			RVBuffered = (float) in.readDouble();
		}
		if (includeName)
		{
			nBuffered = in.readUTF();
			System.out.println(nBuffered);
		}
		
		long csfDataLength = in.readLong();
		csfData = new byte[(int) csfDataLength];
		in.read(csfData);*/
	}

	private void applyToEnytity(Entity entity)
	{
		if(!(entity instanceof EntityImplementation))
			return;
		
		/*EntityImplementation impl = (EntityImplementation)entity;
		impl.pos.x = XBuffered;
		impl.pos.y = YBuffered;
		impl.pos.z = ZBuffered;
		if (includeRotation)
		{
			impl.rotH = (float) RHBuffered;
			impl.rotV = (float) RVBuffered;
		}
		if (includeName)
		{
			//System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+nBuffered);
			if (entity instanceof EntityNameable)
				((EntityNameable) entity).setName(nBuffered);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(csfData);
		DataInputStream dis = new DataInputStream(bais);
		try
		{
			entity.loadCSF(dis);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+XBuffered);*/
	}

	private void createFryomEntity(Entity entity)
	{
		entityTypeID = entity.getEID();
		entityUUID = entity.getUUID();

		/*Location loc = entity.getLocation();
		XBuffered = loc.x;
		YBuffered = loc.y;
		ZBuffered = loc.z;
		if (includeRotation && entity instanceof EntityImplementation)
		{
			EntityImplementation impl = (EntityImplementation)entity;
			RHBuffered = impl.rotH;
			RVBuffered = impl.rotV;
		}
		if (includeName)
		{
			if (entity instanceof EntityNameable)
				nBuffered = ((EntityNameable) entity).getName();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			entity.saveCSF(dos);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		csfData = baos.toByteArray();*/
	}

	private void process(PacketsProcessor processor)
	{
		if(processor.isClient)
		{
			EntityImplementation entity = (EntityImplementation) Client.world.getEntityByUUID(this.entityUUID);
			//if(this.deleteFlag)
			//	Client.world.removeEntity(entity);
			//else
			{
				//Create an entity if the servers tells you to do so
				/*if(entity == null)
				{
					entity = (EntityImplementation) EntitiesList.newEntity(Client.world, this.entityTypeID);
					entity.entityUUID = this.entityUUID;
					this.applyToEntity(entity);
					Client.world.addEntity(entity);
					//
					
				}
				else
					this.applyToEntity(entity);*/
				
				//Moved here so we can tell the client to control an already existing entity
				/*if(this.defineControl)
				{
					if(entity != null)
					{
						Client.controlledEntity = entity;
						if(entity instanceof EntityControllable)
							((EntityControllable) entity).setController(Client.getInstance());
					}
					//else
					//	Client.getInstance().printChat("Error: Server gave control of unknown entity: "+entityUUID);
				}*/
			}
		}
		else
		{
			//Client isn't allowed to force spawning or moving of anything but himself
			
			//if (processor.getServerClient().getProfile().getControlledEntity() != null && entityUUID == processor.getServerClient().getProfile().getControlledEntity().getUUID())
			//	applyToEntity(processor.getServerClient().getProfile().getControlledEntity());
			
			//entity = EntitiesList.newEntity(world, entityType);
		}
	}
}
