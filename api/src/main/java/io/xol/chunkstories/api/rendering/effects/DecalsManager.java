package io.xol.chunkstories.api.rendering.effects;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface DecalsManager
{
	public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName);
}
