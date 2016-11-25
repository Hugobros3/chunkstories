package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * We don't want controlled entities to lag-out when the master-side applies momentum to them, thus we use a special packet for this corner case.
 */
public class PacketVelocityDelta extends PacketSynchPrepared
{
	public PacketVelocityDelta()
	{
		
	}
	
	public PacketVelocityDelta(Vector3d delta)
	{
		this.delta = delta;
	}
	
	private Vector3d delta;
	
	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeDouble(delta.getX());
		out.writeDouble(delta.getY());
		out.writeDouble(delta.getZ());
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		delta = new Vector3d(delta.getX(), delta.getY(), delta.getZ());
		
		EntityControllable entity = Client.getInstance().getClientSideController().getControlledEntity();
		if(entity != null)
		{
			System.out.println("Debug: received velocity delta "+delta);
			entity.getVelocityComponent().addVelocity(delta);
		}
	}

}
