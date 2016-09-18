package io.xol.chunkstories.api.rendering.effects;

import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface DecalsManager
{
	public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, String decalName);
}
