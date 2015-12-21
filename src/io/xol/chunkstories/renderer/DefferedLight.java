package io.xol.chunkstories.renderer;

import org.lwjgl.util.vector.Vector3f;
import static io.xol.chunkstories.renderer.DefferedLight.DefferedLightType.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class DefferedLight
{
	public Vector3f color;
	public Vector3f position;

	public float decay;

	public DefferedLightType type;

	public float angle;
	public Vector3f direction;

	public DefferedLight(Vector3f color, Vector3f position, float decay)
	{
		this.color = color;
		this.position = position;
		this.decay = decay;
		type = POINT;
	}

	public DefferedLight(Vector3f color, Vector3f position, float decay,
			float angle, Vector3f direction)
	{
		this.color = color;
		this.position = position;
		this.decay = decay;
		this.angle = angle;
		this.direction = direction;
		type = SPOT;
	}

	public enum DefferedLightType
	{
		POINT, SPOT;
	}
}
