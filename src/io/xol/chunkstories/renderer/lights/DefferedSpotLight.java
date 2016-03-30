package io.xol.chunkstories.renderer.lights;

import org.lwjgl.util.vector.Vector3f;

import io.xol.chunkstories.api.rendering.SpotLight;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefferedSpotLight extends DefferedLight implements SpotLight
{
	public DefferedSpotLight(Vector3f color, Vector3f position, float decay, float angle, Vector3f direction)
	{
		super(color, position, decay);
		this.angle = angle;
		this.direction = direction;
	}

	public float angle;
	public Vector3f direction;

	@Override
	public float getAngle()
	{
		return angle;
	}

	@Override
	public void setAngle(float angle)
	{
		this.angle = angle;
	}

	@Override
	public Vector3f getDirection()
	{
		return direction;
	}

	@Override
	public void setDirection(Vector3f direction)
	{
		this.direction = direction;
	}
}
