package io.xol.chunkstories.api.rendering;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;

public interface CameraInterface
{
	public boolean isBoxInFrustrum(Vector3f center, Vector3f dimensions);

	public void setupShader(ShaderProgram shaderProgram);

	public Vector3f getViewDirection();

	public Vector3d getCameraPosition();
}