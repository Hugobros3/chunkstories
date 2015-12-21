package io.xol.engine.sound;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SoundSource
{
	public SoundSource(SoundData data, float x, float y, float z, boolean l, float p, float g)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		gain = g;
		pitch = p;
		loop = l;
		soundData = data;
	}
	
	public long internalID;
	public int alId;
	
	public float x, y, z;
	
	public float pitch;
	
	public float gain;
	
	public boolean loop = false;
	
	public boolean isDonePlaying()
	{
		return !loop && !soundData.hasDataRemaining();
	}
	
	public SoundData soundData;
	
	public void onRemove()
	{
		
	}
}
