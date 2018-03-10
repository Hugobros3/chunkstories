//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.sound.ogg;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.openal.AL10.*;

import io.xol.chunkstories.api.content.Asset;
import io.xol.engine.sound.SoundData;



public class SoundDataOggSample extends SoundData
{
	int alId = -1;
	
	public SoundDataOggSample(Asset asset)
	{
		if(asset != null)
		{
			try
			{
				ByteArrayOutputStream oggData = new ByteArrayOutputStream();
				OggInputStream oggInput = new OggInputStream(new DataInputStream(asset.read()));
				while (!oggInput.atEnd()) {
					oggData.write(oggInput.read());
				}
				oggInput.close();
				byte[] lel = oggData.toByteArray();
				ByteBuffer buf = BufferUtils.createByteBuffer(lel.length);
				buf.put(lel);
				buf.flip();
				alId = alGenBuffers();
				length = lel.length * 1000L / (oggInput.info.channels * oggInput.getRate());
				//System.out.println("Sound "+f+" is "+length+" ms long."+oggInput.info.channels+"r:"+oggInput.getRate()+"lel"+lel.length);
				int format = oggInput.info.channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
				alBufferData(alId, format, buf, oggInput.getRate());
				int result;
				if((result = alGetError()) != AL_NO_ERROR)
					System.out.println(getALErrorString(result));
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String getALErrorString(int err) {
		  switch (err) {
		    case AL_NO_ERROR:
		      return "AL_NO_ERROR";
		    case AL_INVALID_NAME:
		      return "AL_INVALID_NAME";
		    case AL_INVALID_ENUM:
		      return "AL_INVALID_ENUM";
		    case AL_INVALID_VALUE:
		      return "AL_INVALID_VALUE";
		    case AL_INVALID_OPERATION:
		      return "AL_INVALID_OPERATION";
		    case AL_OUT_OF_MEMORY:
		      return "AL_OUT_OF_MEMORY";
		    default:
		      return "No such error code";
		  }
		}
	
	@Override
	public int getBuffer()
	{
		return alId;
	}
	
	@Override
	public void destroy()
	{
		alDeleteBuffers(alId);
	}

	long length = -1;
	public String name = "undefined";
	
	@Override
	public long getLengthMs()
	{
		return length;
	}

	@Override
	public boolean loadedOk()
	{
		return length != -1;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
