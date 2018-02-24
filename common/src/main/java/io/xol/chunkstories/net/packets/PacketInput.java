//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.player.PlayerInputPressedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputReleasedEvent;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.input.InputVirtual;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorld;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Transfers client's input to the server
 */
public class PacketInput extends PacketWorld {
	public Input input;
	public boolean isPressed;
	
	public PacketInput(World world) {
		super(world);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeLong(input.getHash());
		
		out.writeBoolean(isPressed);
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		long code = in.readLong();
		boolean pressed = in.readBoolean();

		if (processor instanceof ServerPlayerPacketsProcessor) {
			ServerPlayerPacketsProcessor sppc = (ServerPlayerPacketsProcessor) processor;

			// Look for the controller handling this buisness
			EntityControllable entity = (EntityControllable) sppc.getPlayer().getControlledEntity();
			
			if (entity != null) {
				// Get input of the client
				input = sppc.getPlayer().getInputsManager().getInputFromHash(code);

				if (input == null)
					throw new NullPointerException("Unknown input hash : " + code);

				// System.out.println(processor.getServerClient().getProfile() +
				// " "+input + " "+pressed);
				// Update it's state
				((InputVirtual) input).setPressed(pressed);

				// Fire appropriate event
				if (pressed) {
					PlayerInputPressedEvent event = new PlayerInputPressedEvent(
							sppc.getPlayer(), input);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

					if (!event.isCancelled())
						entity.onControllerInput(input, entity.getControllerComponent().getController());
				} else {
					PlayerInputReleasedEvent event = new PlayerInputReleasedEvent(
							sppc.getPlayer(), input);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);
				}

				//TODO why is this disabled and still there ?
				// If we pressed the input, apply game logic
				// if(pressed)
				// entity.handleInteraction(input,
				// entity.getControllerComponent().getController());
			}
		}
	}
}
