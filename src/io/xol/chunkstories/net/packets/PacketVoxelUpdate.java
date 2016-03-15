package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a voxel change
 * @author gobrosse
 *
 */
public class PacketVoxelUpdate extends Packet
{
	public int x, y, z;
	public int data;
	
	public PacketVoxelUpdate(boolean client)
	{
		super(client);
	}

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeInt(data);
		//No further information
		out.writeByte((byte)0x00);
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		data = in.readInt();
		byte osef = in.readByte();
		assert osef == 0x00;
	}

	@Override
	public void process(PacketsProcessor processor)
	{
		if(Client.world instanceof WorldClient)
		{
			Client.world.setDataAt(x, y, z, data);
		}
	}

}
