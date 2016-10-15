package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.categories.ClientEvent;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.net.packets.PacketInput;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientInputPressedEvent extends CancellableEvent implements ClientEvent
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners();

	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}

	public static EventListeners getListenersStatic()
	{
		return listeners;
	}

	// Specific event code

	public ClientInputPressedEvent(Input input)
	{
		this.input = input;
	}

	Input input;

	public Input getInput()
	{
		return input;
	}

	public void defaultBehaviour()
	{
		if (!this.isCancelled())
		{
			final EntityControllable entityControlled = Client.getInstance().getClientSideController().getControlledEntity();

			//There has to be a controlled entity for sending inputs to make sense.
			if(entityControlled == null)
				return;
			
			//Send input to server
			if (entityControlled instanceof WorldClientRemote)
			{
				ClientToServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
				PacketInput packet = new PacketInput(true);
				packet.input = input;
				connection.sendPacket(packet);
			}

			//Handle interaction locally
			if (entityControlled != null)
				entityControlled.handleInteraction(input, Client.getInstance().getClientSideController());
		}
	}

}
