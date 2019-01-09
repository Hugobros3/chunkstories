//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.events.player.PlayerInputPressedEvent;
import xyz.chunkstories.api.events.player.PlayerInputReleasedEvent;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.net.PacketDestinator;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.api.net.PacketSendingContext;
import xyz.chunkstories.api.net.PacketWorld;
import xyz.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.input.InputVirtual;

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
						entity.traits.with(TraitControllable.class, ec -> {
							Controller controller = ec.getController();
							//TODO fix
							//entity.traits.with(TraitWhenControlled.class, t -> {
							//	t.onControllerInput(input, controller);
							//});
						});
				} else {
					PlayerInputReleasedEvent event = new PlayerInputReleasedEvent(sppc.getPlayer(), input);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);
				}
			}
		}
	}
}
