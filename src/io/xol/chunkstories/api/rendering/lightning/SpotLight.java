package io.xol.chunkstories.api.rendering.lightning;

import io.xol.engine.math.lalgb.vector.Vector3;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SpotLight extends Light
{
	public SpotLight(Vector3<Float> color, Vector3<Float> position, float decay, float angle, Vector3<Float> direction)
	{
		super(color, position, decay);
		this.angle = angle;
		this.direction = direction;
	}

	public float angle;
	public Vector3<Float> direction;

	public float getAngle()
	{
		return angle;
	}

	public void setAngle(float angle)
	{
		this.angle = angle;
	}

	public Vector3<Float> getDirection()
	{
		return direction;
	}

	public void setDirection(Vector3<Float> direction)
	{
		this.direction = direction;
	}
}
