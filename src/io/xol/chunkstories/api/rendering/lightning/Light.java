package io.xol.chunkstories.api.rendering.lightning;

import io.xol.chunkstories.api.math.vector.Vector3;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Light
{
	public Vector3<Float> color;
	public Vector3<Float> position;
	public float decay;

	public Light(Vector3<Float> color, Vector3<Float> position, float decay)
	{
		this.color = color;
		this.position = position;
		this.decay = decay;
	}

	public Vector3<Float> getColor()
	{
		return color;
	}
	
	public void setColor(Vector3<Float> color)
	{
		this.color = color;
	}

	public Vector3<Float> getPosition()
	{
		return position;
	}

	public void setPosition(Vector3<Float> position)
	{
		this.position = position;
	}

	public float getDecay()
	{
		return decay;
	}

	public void setDecay(float decay)
	{
		this.decay = decay;
	}
}
