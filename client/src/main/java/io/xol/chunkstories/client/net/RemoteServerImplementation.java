//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.RemoteServer;

/** 
 * Represents the Remote server from the client point of view. 
 * See RemoteServer in the API for more details on the concept behind this.
 */
public class RemoteServerImplementation implements RemoteServer {

	final ServerConnection connection;
	
	public RemoteServerImplementation(ServerConnection connection) {
		this.connection = connection;
	}
	
	@Override
	public long getUUID()
	{
		return -1;
	}

	Set<Entity> controlledEntity = new HashSet<Entity>(1);
	
	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		return controlledEntity.iterator();
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		assert controlledEntity.size() == 0;
		((EntityBase) entity).subscribe(this);
		return controlledEntity.add(entity);
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		assert controlledEntity.size() == 1;
		((EntityBase) entity).unsubscribe(this);
		return controlledEntity.remove(entity);
	}

	@Override
	public void unsubscribeAll()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void pushPacket(Packet packet)
	{
		this.connection.pushPacket(packet);
	}

	public boolean isSubscribedTo(Entity entity)
	{
		return controlledEntity.contains(entity);
	}

	@Override
	public void flush() {
		this.connection.flush();
	}

	@Override
	public void disconnect() {
		this.connection.close();
	}

	@Override
	public void disconnect(String disconnectionReason) {
		//TODO send reason if possible
		this.connection.close();
	}
}
