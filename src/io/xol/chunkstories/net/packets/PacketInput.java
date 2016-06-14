package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.input.MouseClick;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.input.KeyBindVirtual;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a voxel change
 * 
 * @author gobrosse
 */
public class PacketInput extends Packet
{
	public Input input;

	public PacketInput(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		if (input instanceof MouseClick)
		{
			if (input.equals(MouseClick.LEFT))
				out.writeLong(0x01);
			else if (input.equals(MouseClick.MIDDLE))
				out.writeLong(0x02);
			else if (input.equals(MouseClick.RIGHT))
				out.writeLong(0x03);
		}
		//TODO use unique hash codes instead of sending the whole thing
		else if (input instanceof KeyBind)
		{
			out.writeLong(0x00);
			out.writeUTF(input.getName());
			//out.writeLong(((KeyBind) input).getHash());
			//System.out.println("sent: "+input+" code"+((KeyBindImplementation) input).getHash());
		}
	}
	
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		long code = in.readLong();
		if (code == 0x01)
			input = MouseClick.LEFT;
		else if (code == 0x02)
			input = MouseClick.MIDDLE;
		else if (code == 0x03)
			input = MouseClick.RIGHT;
		else if(code == 0x00)
		{
			input = new KeyBindVirtual(in.readUTF());
		}
		//input = KeyBinds.getKeyBindFromHash(code);
		//System.out.println("received input: "+input+" code"+code);
	}

	public void process(PacketsProcessor processor)
	{
		EntityControllable entity = (EntityControllable) processor.getServerClient().getProfile().getControlledEntity();
		if (entity != null)
		{
			entity.handleInteraction(input);
			//System.out.println("handle interaction");
		}
	}

}
