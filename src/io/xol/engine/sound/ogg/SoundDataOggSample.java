package io.xol.engine.sound.ogg;

import java.io.File;

import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;

import io.xol.engine.sound.SoundData;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SoundDataOggSample extends SoundData
{
	public SoundDataOggSample(File f)
	{
		if(f.exists())
		{
			try
			{
				VorbisFile file = new VorbisFile(f.getAbsolutePath());
				//System.out.println(file.seekable()+"f"+file.pcm_tell());
			}
			catch (JOrbisException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public byte[] getData()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasDataRemaining()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
