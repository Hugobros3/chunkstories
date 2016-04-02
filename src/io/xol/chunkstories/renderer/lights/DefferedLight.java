package io.xol.chunkstories.renderer.lights;

import io.xol.engine.math.lalgb.Vector3f;

import io.xol.chunkstories.api.rendering.Light;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class DefferedLight implements Light
{
	public Vector3f color;
	public Vector3f position;
	public float decay;

	public DefferedLight(Vector3f color, Vector3f position, float decay)
	{
		this.color = color;
		this.position = position;
		this.decay = decay;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.Light#getColor()
	 */
	@Override
	public Vector3f getColor()
	{
		return color;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.Light#setColor(io.xol.engine.math.lalgb.Vector3f)
	 */
	@Override
	public void setColor(Vector3f color)
	{
		this.color = color;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.Light#getPosition()
	 */
	@Override
	public Vector3f getPosition()
	{
		return position;
	}

	@Override
	public void setPosition(Vector3f position)
	{
		this.position = position;
	}

	@Override
	public float getDecay()
	{
		return decay;
	}

	@Override
	public void setDecay(float decay)
	{
		this.decay = decay;
	}
}
