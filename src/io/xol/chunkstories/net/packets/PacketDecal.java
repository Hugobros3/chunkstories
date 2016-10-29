package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketDecal extends PacketSynchPrepared
{
	public String decalName;
	public Vector3d position;
	public Vector3d orientation;
	public Vector3d size;

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
		//System.out.println("cuck");
		
		decalName = in.readUTF();
		position = new Vector3d();
		position.setX(in.readDouble());
		position.setY(in.readDouble());
		position.setZ(in.readDouble());

		orientation = new Vector3d();
		orientation.setX(in.readDouble());
		orientation.setY(in.readDouble());
		orientation.setZ(in.readDouble());

		size = new Vector3d();
		size.setX(in.readDouble());
		size.setY(in.readDouble());
		size.setZ(in.readDouble());
		
		Client.getInstance().getDecalsManager().drawDecal(position, orientation, size, decalName);
	}

}
