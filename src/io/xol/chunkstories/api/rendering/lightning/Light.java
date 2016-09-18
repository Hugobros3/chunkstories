package io.xol.chunkstories.api.rendering.lightning;

import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Light
{
	public Vector3f color;
	public Vector3f position;
	public float decay;

	public Light(Vector3f color, Vector3f position, float decay)
	{
		this.color = color;
		this.position = position;
		this.decay = decay;
	}

	public Vector3f getColor()
	{
		return color;
	}
	
	public void setColor(Vector3f color)
	{
		this.color = color;
	}

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f position)
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
