package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketParticle extends PacketSynchPrepared
{
	public String particleName = "";
	public Vector3dm position;
	public Vector3dm velocity;

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeUTF(particleName);
		out.writeDouble(position.getX());
		out.writeDouble(position.getY());
		out.writeDouble(position.getZ());
		out.writeBoolean(velocity != null);
		if (velocity != null)
		{
			out.writeDouble(velocity.getX());
			out.writeDouble(velocity.getY());
			out.writeDouble(velocity.getZ());
		}
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		particleName = in.readUTF();
		position = new Vector3dm();
		position.setX(in.readDouble());
		position.setY(in.readDouble());
		position.setZ(in.readDouble());
		if(in.readBoolean())
		{
			velocity = new Vector3dm();
			velocity.setX(in.readDouble());
			velocity.setY(in.readDouble());
			velocity.setZ(in.readDouble());
		}
		
		Client.getInstance().getParticlesManager().spawnParticleAtPositionWithVelocity(particleName, position, velocity);
	}

}
