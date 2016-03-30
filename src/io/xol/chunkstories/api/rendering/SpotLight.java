package io.xol.chunkstories.api.rendering;

import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface SpotLight extends Light
{

	float getAngle();

	void setAngle(float angle);

	Vector3f getDirection();

	void setDirection(Vector3f direction);
}
