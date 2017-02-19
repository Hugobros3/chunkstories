package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.physics.CollisionBox;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface CameraInterface
{
	public boolean isBoxInFrustrum(Vector3<Float> center, Vector3<Float> dimensions);
	
	public boolean isBoxInFrustrum(CollisionBox box);

	public void setupShader(ShaderInterface shaderProgram);

	public Vector3<Double> getCameraPosition();
	
	public void setCameraPosition(Vector3dm pos);

	public Vector3fm getViewDirection();
	
	public float getFOV();
	
	public void setFOV(float fov);

	public Vector3fm transform3DCoordinate(Vector3fm vector3f);

	void setRotationZ(float rotationZ);

	float getRotationZ();

	void setRotationY(float rotationY);

	float getRotationY();

	void setRotationX(float rotationX);

	float getRotationX();

	public void setupUsingScreenSize(int width, int height);
}