package io.xol.chunkstories.api.rendering.lightning;

import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SpotLight extends Light
{
	public SpotLight(Vector3f color, Vector3f position, float decay, float angle, Vector3f direction)
	{
		super(color, position, decay);
		this.angle = angle;
		this.direction = direction;
	}

	public float angle;
	public Vector3f direction;

	public float getAngle()
	{
		return angle;
	}

	public void setAngle(float angle)
	{
		this.angle = angle;
	}

	public Vector3f getDirection()
	{
		return direction;
	}

	public void setDirection(Vector3f direction)
	{
		this.direction = direction;
	}
}
