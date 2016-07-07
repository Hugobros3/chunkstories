package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.input.InputVirtual;

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
		out.writeLong(input.getHash());
		out.writeBoolean(input.isPressed());
	}
	
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		long code = in.readLong();
		boolean pressed = in.readBoolean();
		
		//Look for the controller handling this buisness
		EntityControllable entity = (EntityControllable) processor.getServerClient().getProfile().getControlledEntity();
		if (entity != null)
		{
			//Get input of the client
			input = processor.getServerClient().getProfile().getInputsManager().getInputFromHash(code);
			
			if(input == null)
				throw new NullPointerException();
			
			//System.out.println(processor.getServerClient().getProfile() + " "+input + " "+pressed);
			//Update it's state
			((InputVirtual)input).setPressed(pressed);
			
			//If we pressed the input, apply game logic
			if(pressed)
				entity.handleInteraction(input, entity.getControllerComponent().getController());
		}
	}
}
