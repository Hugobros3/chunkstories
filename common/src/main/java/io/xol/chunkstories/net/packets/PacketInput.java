//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitWhenControlled;
import io.xol.chunkstories.api.entity.traits.serializable.TraitController;
import io.xol.chunkstories.api.events.player.PlayerInputPressedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputReleasedEvent;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.input.InputVirtual;

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
			Entity entity = sppc.getPlayer().getControlledEntity();

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
					PlayerInputPressedEvent event = new PlayerInputPressedEvent(sppc.getPlayer(), input);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

					if (!event.isCancelled())
						entity.traits.with(TraitController.class, ec -> {
							Controller controller = ec.getController();
							entity.traits.with(TraitWhenControlled.class, t -> {
								t.onControllerInput(input, controller);
							});
						});

					// entity.onControllerInput(input,
					// entity.getControllerComponent().getController());
				} else {
					PlayerInputReleasedEvent event = new PlayerInputReleasedEvent(sppc.getPlayer(), input);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);
				}

				// TODO why is this disabled and still there ?
				// If we pressed the input, apply game logic
				// if(pressed)
				// entity.handleInteraction(input,
				// entity.getControllerComponent().getController());
			}
		}
	}
}
