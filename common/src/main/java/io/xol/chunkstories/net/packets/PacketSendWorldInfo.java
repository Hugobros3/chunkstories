package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.world.WorldInfoImplementation;

import java.io.ByteArrayOutputStream;
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
		//This is moronic
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		info.saveInStream(baos);
		
		//So is this
		baos.flush();
		byte[] fuckthis = baos.toByteArray();
		
		//And all of this
		out.writeInt(fuckthis.length);
		out.write(fuckthis);
		
		//I especially hated this part
		out.flush();
		
		//Wasted half an afternoon trying to figure out this mess, moral of the story is to NEVER bother hacking arround with utf-8 and ALWAYS send the length before, because fuck
		//you that's why.
		
		//System.out.println("Sent "+fuckthis.length +" bytes of world data to "+destinator);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		
	}
}
