package io.xol.chunkstories.api.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.net.PacketSender;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Simply sends a decal to the client to be drawn */
public class PacketDecal extends PacketSynchPrepared
{
	public String decalName;
	public Vector3dm position;
	public Vector3dm orientation;
	public Vector3dm size;

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeUTF(decalName);
		out.writeDouble(position.getX());
		out.writeDouble(position.getY());
		out.writeDouble(position.getZ());
		out.writeDouble(orientation.getX());
		out.writeDouble(orientation.getY());
		out.writeDouble(orientation.getZ());
		out.writeDouble(size.getX());
		out.writeDouble(size.getY());
		out.writeDouble(size.getZ());
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		decalName = in.readUTF();
		position = new Vector3dm();
		position.setX(in.readDouble());
		position.setY(in.readDouble());
		position.setZ(in.readDouble());

		orientation = new Vector3dm();
		orientation.setX(in.readDouble());
		orientation.setY(in.readDouble());
		orientation.setZ(in.readDouble());

		size = new Vector3dm();
		size.setX(in.readDouble());
		size.setY(in.readDouble());
		size.setZ(in.readDouble());
		
		if(processor instanceof ClientPacketsProcessor)
		{
			ClientPacketsProcessor cpp = (ClientPacketsProcessor)processor;
			cpp.getContext().getDecalsManager().drawDecal(position, orientation, size, decalName);
		}
	}

}
