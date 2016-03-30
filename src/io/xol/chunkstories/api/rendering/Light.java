package io.xol.chunkstories.api.rendering;

import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Light
{
	Vector3f getColor();

	void setColor(Vector3f color);

	Vector3f getPosition();

	void setPosition(Vector3f position);

	float getDecay();

	void setDecay(float decay);
}