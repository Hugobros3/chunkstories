//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import xyz.chunkstories.api.client.net.ClientPacketsProcessor;
import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.net.PacketDestinator;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.api.net.PacketSendingContext;
import xyz.chunkstories.api.net.PacketWorld;
import xyz.chunkstories.api.sound.SoundSource;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.sound.source.SoundSourceVirtual;

public class PacketSoundSource extends PacketWorld {
	public SoundSourceVirtual soundSourceToSend;

	public PacketSoundSource(World world) {
		super(world);
	}

	public PacketSoundSource(World world, SoundSourceVirtual soundSource) {
		this(world);
		this.soundSourceToSend = soundSource;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeUTF(soundSourceToSend.getName());
		out.writeLong(soundSourceToSend.getUuid());
		Vector3dc position = soundSourceToSend.getPosition();
		if (position != null) {
			out.writeBoolean(true);
			out.writeFloat((float) position.x());
			out.writeFloat((float) position.y());
			out.writeFloat((float) position.z());
		} else
			out.writeBoolean(false);
		// out.writeBoolean(soundSourceToSend.loop);
		// out.writeBoolean(soundSourceToSend.isAmbient);
		// out.writeBoolean(soundSourceToSend.buffered);
		out.writeByte(soundSourceToSend.getMode().ordinal());
		out.writeBoolean(soundSourceToSend.isDonePlaying());
		out.writeFloat(soundSourceToSend.getPitch());
		out.writeFloat(soundSourceToSend.getGain());
		out.writeFloat(soundSourceToSend.getAttenuationStart());
		out.writeFloat(soundSourceToSend.getAttenuationEnd());
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor)
			throws IOException, PacketProcessingException {
		String soundName = in.readUTF();
		long UUID = in.readLong();

		boolean hasPosition = in.readBoolean();
		Vector3dc position = null;
		if (hasPosition) {
			position = new Vector3d(in.readFloat(), in.readFloat(), in.readFloat());
		}

		// boolean loop = in.readBoolean();
		// boolean isAmbient = in.readBoolean();
		// boolean buffered = in.readBoolean();

		byte modeByte = in.readByte();
		Mode mode = Mode.values()[modeByte];

		boolean stopped = in.readBoolean();
		float pitch = in.readFloat();
		float gain = in.readFloat();
		float attenuationStart = in.readFloat();
		float attenuationEnd = in.readFloat();

		if (!(processor instanceof ClientPacketsProcessor))
			return;

		ClientPacketsProcessor cpe = (ClientPacketsProcessor) processor;

		SoundSource soundSource = cpe.getContext().getSoundManager().getSoundSourceByUUID(UUID);

		// ALSoundSource soundSource = (ALSoundSource)
		// Client.getInstance().getSoundManager().getSoundSourceByUUID(UUID);
		if (soundSource == null && stopped)
			return;

		if (soundSource == null) {

			soundSource = cpe.getContext().getSoundManager().replicateServerSoundSource(soundName, mode, position,
					pitch, gain, attenuationStart, attenuationEnd, UUID);
			return;
		}
		if (stopped) {
			soundSource.stop();
			return;
		}

		// Update the soundSource with all we can
		soundSource.setPosition(position);
		soundSource.setPitch(pitch);
		soundSource.setGain(gain);
		soundSource.setAttenuationStart(attenuationStart);
		soundSource.setAttenuationEnd(attenuationEnd);
	}

}
