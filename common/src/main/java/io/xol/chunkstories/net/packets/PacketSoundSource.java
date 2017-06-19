package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.engine.sound.sources.SoundSourceVirtual;
import io.xol.chunkstories.api.net.PacketSender;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketSoundSource extends PacketSynchPrepared
{
	public SoundSourceVirtual soundSourceToSend;

	public PacketSoundSource()
	{
		
	}
	
	public PacketSoundSource(SoundSourceVirtual soundSource)
	{
		this.soundSourceToSend = soundSource;
	}

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeUTF(soundSourceToSend.getSoundName());
		out.writeLong(soundSourceToSend.soundSourceUUID);
		out.writeFloat(soundSourceToSend.x);
		out.writeFloat(soundSourceToSend.y);
		out.writeFloat(soundSourceToSend.z);
		out.writeBoolean(soundSourceToSend.loop);
		out.writeBoolean(soundSourceToSend.isAmbient);
		out.writeBoolean(soundSourceToSend.buffered);
		out.writeBoolean(soundSourceToSend.stopped);
		out.writeFloat(soundSourceToSend.pitch);
		out.writeFloat(soundSourceToSend.gain);
		out.writeFloat(soundSourceToSend.attenuationStart);
		out.writeFloat(soundSourceToSend.attenuationEnd);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		String soundName = in.readUTF();
		long UUID = in.readLong();
		float x = in.readFloat();
		float y = in.readFloat();
		float z = in.readFloat();
		boolean loop = in.readBoolean();
		boolean isAmbient = in.readBoolean();
		boolean buffered = in.readBoolean();
		boolean stopped = in.readBoolean();
		float pitch = in.readFloat();
		float gain = in.readFloat();
		float attenuationStart = in.readFloat();
		float attenuationEnd = in.readFloat();

		if(!(processor instanceof ClientPacketsProcessor))
			return;
		
		ClientPacketsProcessor cpe = (ClientPacketsProcessor)processor;
		
		SoundSource soundSource = cpe.getContext().getSoundManager().getSoundSourceByUUID(UUID);
		
		//ALSoundSource soundSource = (ALSoundSource) Client.getInstance().getSoundManager().getSoundSourceByUUID(UUID);
		if (soundSource == null && stopped)
			return;

		if (soundSource == null)
			
			cpe.getContext().getSoundManager().replicateServerSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd, buffered, UUID);

			
			/*try
			{
				/*if (buffered)
					soundSource = new ALBufferedSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd);
				else
					soundSource = new ALSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd);
				
				//Match the UUIDs
				soundSource.setUUID(UUID);
				((ALSoundManager) Client.getInstance().getSoundManager()).addSoundSource(soundSource);*/
				
				//Play dat shit dawg
			/*	cpe.getContext().getSoundManager().replicateServerSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd, buffered, UUID);
				
			}
			catch (SoundEffectNotFoundException e)
			{
				cpe.getContext().logger().error("Can't play sound "+soundName + "from server. (UUID="+UUID+")");
				return;
			}*/
		
		if(stopped)
		{
			soundSource.stop();
			return;
		}

		//Update the soundSource with all we can
		soundSource.setPosition(x, y, z);
		soundSource.setPitch(pitch);
		soundSource.setGain(gain);
		soundSource.setAttenuationStart(attenuationStart);
		soundSource.setAttenuationEnd(attenuationEnd);
	}

}
