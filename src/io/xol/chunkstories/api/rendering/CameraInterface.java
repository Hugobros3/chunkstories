package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface CameraInterface
{
	public boolean isBoxInFrustrum(Vector3<Float> center, Vector3<Float> dimensions);

	public void setupShader(ShaderInterface shaderProgram);

	public Vector3fm getViewDirection();

	public Vector3dm getCameraPosition();

	public Vector3fm transform3DCoordinate(Vector3fm vector3f);
}