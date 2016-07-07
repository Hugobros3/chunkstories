package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.input.MouseButton;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.input.KeyBindVirtual;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Transfers client's input to the server
 */
public class PacketInput extends PacketSynch
{
	public Input input;

	public PacketInput(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		if (input instanceof MouseButton)
		{
			if (input.equals(MouseButton.LEFT))
				out.writeLong(0x01);
			else if (input.equals(MouseButton.MIDDLE))
				out.writeLong(0x02);
			else if (input.equals(MouseButton.RIGHT))
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

		EntityControllable entity = (EntityControllable) processor.getServerClient().getProfile().getControlledEntity();
		if (entity != null)
		{
			entity.handleInteraction(input, entity.getControllerComponent().getController());
			//System.out.println("handle interaction");
		}
	}
	
	public void read(DataInputStream in) throws IOException
	{
		long code = in.readLong();
		if (code == 0x01)
			input = MouseButton.LEFT;
		else if (code == 0x02)
			input = MouseButton.MIDDLE;
		else if (code == 0x03)
			input = MouseButton.RIGHT;
		else if(code == 0x00)
		{
			input = new KeyBindVirtual(in.readUTF());
		}
		//input = KeyBinds.getKeyBindFromHash(code);
		//System.out.println("received input: "+input+" code"+code);
	}
}
