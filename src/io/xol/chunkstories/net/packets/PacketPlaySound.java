package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketPlaySound extends PacketSynch
{
	public String soundName;
	public Vector3d position;
	public float pitch;
	public float gain;
	
	public PacketPlaySound(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeUTF(soundName);
		out.writeDouble(position.x);
		out.writeDouble(position.y);
		out.writeDouble(position.z);
		out.writeFloat(pitch);
		out.writeFloat(gain);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		soundName = in.readUTF();
		position = new Vector3d(in.readDouble(), in.readDouble(), in.readDouble());
		pitch = in.readFloat();
		gain = in.readFloat();
		
		if(processor.isClient)
			Client.getInstance().getSoundManager().playSoundEffect(soundName, position, pitch, gain);
		
		System.out.println("rcvd snd");
	}

}
