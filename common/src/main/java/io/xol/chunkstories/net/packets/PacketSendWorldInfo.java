package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.world.WorldInfoImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketSendWorldInfo extends Packet
{
	public WorldInfoImplementation info;
	
	public PacketSendWorldInfo() {
		
	}
	
	public PacketSendWorldInfo(WorldInfoImplementation info) {
		this.info = info;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		/*String tg = "";
		for (String line : info.saveText())
			tg += line + "\n";
		
		char[] data = tg.toCharArray();
		//Wow such computations
		short length = (short) (data.length * 2);
		byte[] bytes = new byte[length + 2];
		//Write length in first 2 bytes
		bytes[0] = (byte) (length >> 8);
		bytes[1] = (byte) length;
		//Then the data
		for (int i = 0; i < data.length; i++)
		{
			bytes[i * 2 + 2] = (byte) (data[i] >> 8);
			bytes[i * 2 + 3] = (byte) data[i];
		}
		out.write(bytes);*/
		
		info.saveInStream(out);
		
		//Append an 'end' tag so the WorldInfoImplementation reader can figure out it's done reading that
		
		out.write("end\n".getBytes("UTF-8"));
		//out.writeUTF("end\n");
		
		out.flush();
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		
	}
}
