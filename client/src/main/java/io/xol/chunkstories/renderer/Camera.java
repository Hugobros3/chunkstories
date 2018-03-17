//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.physics.CollisionPlane;

public class Camera implements CameraInterface
{
	//Viewport size
	public int viewportWidth, viewportHeight;
	
	//Camera rotations
	public float rotationX = 0.0f;
	public float rotationY = 0.0f;
	public float rotationZ = 0.0f;
	
	private Vector3d position = new Vector3d();
	private Vector3f up = new Vector3f();
	
	//Mouse pointer tracking
	float lastPX = -1f;
	float lastPY = -1f;

	//Matrices
	public Matrix4f projectionMatrix4f = new Matrix4f();
	public Matrix4f projectionMatrix4fInverted = new Matrix4f();

	public Matrix4f modelViewProjectionMatrix4f = new Matrix4f();
	public Matrix4f modelViewProjectionMatrix4fInverted = new Matrix4f();
	
	public Matrix4f untranslatedMVP4f = new Matrix4f();
	public Matrix4f untranslatedMVP4fInv = new Matrix4f();
	
	public Matrix4f modelViewMatrix4f = new Matrix4f();
	public Matrix4f modelViewMatrix4fInverted = new Matrix4f();

	public Matrix3f normalMatrix3f = new Matrix3f();
	public Matrix3f normalMatrix3fInverted = new Matrix3f();

	public Matrix4f getProjectionMatrix4f() {
		return projectionMatrix4f;
	}

	public Matrix4f getProjectionMatrix4fInverted() {
		return projectionMatrix4fInverted;
	}

	public Matrix4f getModelViewProjectionMatrix4f() {
		return modelViewProjectionMatrix4f;
	}

	public Matrix4f getModelViewProjectionMatrix4fInverted() {
		return modelViewProjectionMatrix4fInverted;
	}

	public Matrix4f getUntranslatedMVP4f() {
		return untranslatedMVP4f;
	}

	public Matrix4f getUntranslatedMVP4fInv() {
		return untranslatedMVP4fInv;
	}

	public Matrix4f getModelViewMatrix4f() {
		return modelViewMatrix4f;
	}

	public Matrix4f getModelViewMatrix4fInverted() {
		return modelViewMatrix4fInverted;
	}

	public Matrix3f getNormalMatrix3f() {
		return normalMatrix3f;
	}

	public Matrix3f getNormalMatrix3fInverted() {
		return normalMatrix3fInverted;
	}

	/**
	 * Updates the sound engine position and listener orientation
	 */
	public void alUpdate()
	{
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		
		lookAt.cross(up, up);		
		up.cross(lookAt, up);
		
		Client.getInstance().getSoundManager().setListenerPosition((float)(double)position.x(), (float)(double)position.y(), (float)(double)position.z(), lookAt, up);
	}

	public float fov = 45;

	/**
	 * Computes inverted and derived matrices
	 */
	public void updateMatricesForShaderUniforms()
	{
		//Invert two main patrices
		projectionMatrix4f.invert(projectionMatrix4fInverted);
		
		modelViewMatrix4f.invert(modelViewMatrix4fInverted);
		
		//Build normal matrix
		Matrix4f tempMatrix = new Matrix4f();
		
		modelViewMatrix4f.invert(tempMatrix);
		
		tempMatrix.transpose();
		normalMatrix3f.m00 = tempMatrix.m00();
		normalMatrix3f.m01 = tempMatrix.m01();
		normalMatrix3f.m02 = tempMatrix.m02();

		normalMatrix3f.m10 = tempMatrix.m10();
		normalMatrix3f.m11 = tempMatrix.m11();
		normalMatrix3f.m12 = tempMatrix.m12();

		normalMatrix3f.m20 = tempMatrix.m20();
		normalMatrix3f.m21 = tempMatrix.m21();
		normalMatrix3f.m22 = tempMatrix.m22();
		
		//Invert it
		normalMatrix3f.invert(normalMatrix3fInverted);
		
		//Premultiplied versions ( optimization for poor drivers that don't figure it out themselves )
		projectionMatrix4f.mul(modelViewMatrix4f, modelViewProjectionMatrix4f);
				
		modelViewProjectionMatrix4f.invert(modelViewProjectionMatrix4fInverted);
	}

	public void setupUsingScreenSize(int width, int height)
	{
		this.viewportWidth = width;
		this.viewportHeight = height;
		// Frustrum values
		float fovRad = (float) toRad(fov / 2.0);

		float aspect = (float) width / (float) height;
		float top = (float) Math.tan(fovRad) * 0.1f;
		float bottom = -top;
		float left = aspect * bottom;
		float right = aspect * top;
		float near = 0.1f;
		float far = 3000f;
		
		// Generate the projection matrix
		projectionMatrix4f.identity();
		projectionMatrix4f.m00((near * 2) / (right - left));
		projectionMatrix4f.m11((near * 2) / (top - bottom));
		float A = (right + left) / (right - left);
		float B = (top + bottom) / ( top - bottom);
		float C = - (far + near) / (far - near);
		float D = - (2 * far * near) / (far - near);
		projectionMatrix4f.m20(A);
		projectionMatrix4f.m21(B);
		projectionMatrix4f.m22(C);
		projectionMatrix4f.m32(D);
		projectionMatrix4f.m23(-1);
		projectionMatrix4f.m33(0);
		
		// Grab the generated matrix
		
		modelViewMatrix4f.identity();
		modelViewMatrix4f.rotate((float) (rotationZ / 180 * Math.PI), new Vector3f( 0.0f, 0.0f, 1.0f));
		modelViewMatrix4f.rotate((float) (rotationX / 180 * Math.PI), new Vector3f( 1.0f, 0.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (rotationY / 180 * Math.PI), new Vector3f( 0.0f, 1.0f, 0.0f));
		
		Vector3f position = new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z);
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		up.set(0.0f, 1.0f, 0.0f);
		lookAt.cross(up, up);
		up.cross(lookAt, up);
		
		if(up.x == lookAt.x && lookAt.y == -1.0f && up.z == lookAt.z) {
			//System.out.println("straight down");
			up.set((float) (Math.sin(a)), 0, (float)(Math.cos(a)));
			
		} else if(up.x == -lookAt.x && lookAt.y == 1.0f && up.z == -lookAt.z) {
			//System.out.println("straight up");
			up.set(-(float) (Math.sin(a)), 0, -(float)(Math.cos(a)));
		}
		//System.out.println(up + " : " + lookAt);
		
		modelViewMatrix4f.identity();
		position.mul(0.0f);
		modelViewMatrix4f.lookAt(position, lookAt, up);
		
		lookAt.add(position);
		
		//TODO ?
		position.mul(0.0f);
		
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
		translateCamera();
		alUpdate();
	}

	CollisionPlane[] cameraPlanes = new CollisionPlane[6];
	
	private void computeFrustrumPlanes()
	{
		Vector3f temp = new Vector3f();
		//Init values
		float tang = (float)Math.tan(toRad(fov / 2.0)) ;
		float ratio = (float) viewportWidth / (float) viewportHeight;
		float nh = 0.1f * tang;
		float nw = nh * ratio;
		float fh = 3000f  * tang;
		float fw = fh * ratio;
		
		// Recreate the 3 vectors for the algorithm

		//Vector3m<Float> position = pos.castToSinglePrecision();
		//position = position.negate();
		
		//System.out.println(position);
		
		//Vector3f position = new Vector3f((float)-camPosX, (float)-camPosY, (float)-camPosZ);
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		
		lookAt.cross(up, up);
		//VectorCrossProduct.cross33(lookAt, up, up);
		
		up.cross(lookAt, up);
		//VectorCrossProduct.cross33(up, lookAt, up);
		
		lookAt.add(new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z));
		//Vector3f.add(position, lookAt, lookAt);
		
		// Create the 6 frustrum planes
		Vector3f Z = new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z);
		
		Z.sub(lookAt);
		//Vector3f.sub(position, lookAt, Z);
		Z.normalize();
		
		Vector3f X = new Vector3f();
		
		up.cross(Z, X);
		//VectorCrossProduct.cross33(up, Z, X);
		X.normalize();

		Vector3f Y = new Vector3f();
		
		Z.cross(X, Y);
		//VectorCrossProduct.cross33(Z, X, Y);
		
		Vector3f nearCenterPoint = new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z);
		temp = new Vector3f(Z);
		temp.mul(0.1f);
		
		nearCenterPoint.sub(temp);
		//Vector3f.sub(position, temp, nearCenterPoint);

		Vector3f farCenterPoint = new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z);
		temp = new Vector3f(Z);
		temp.mul(3000f);
		
		farCenterPoint.sub(temp);
		//Vector3f.sub(position, temp, farCenterPoint);
		
		// Eventually the fucking points
		Vector3f nearTopLeft = vadd(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));
		Vector3f nearTopRight = vadd(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3f nearBottomLeft = vsub(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3f nearBottomRight = vsub(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));

		Vector3f farTopLeft = vadd(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		Vector3f farTopRight = vadd(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3f farBottomLeft = vsub(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3f farBottomRight = vsub(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		
		cameraPlanes[0] = new CollisionPlane(nearTopRight, nearTopLeft, farTopLeft);
		cameraPlanes[1] = new CollisionPlane(nearBottomLeft, nearBottomRight, farBottomRight);
		cameraPlanes[2] = new CollisionPlane(nearTopLeft, nearBottomLeft, farBottomLeft);
		cameraPlanes[3] = new CollisionPlane(nearBottomRight, nearTopRight, farBottomRight);
		cameraPlanes[4] = new CollisionPlane(nearTopLeft, nearTopRight, nearBottomRight);
		cameraPlanes[5] = new CollisionPlane(farTopRight, farTopLeft, farBottomLeft);
		
		//cache that
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				for(int k = 0; k < 2; k++)
				{
					corners[i * 4 + j * 2 + k] = new Vector3f();
				}
			}
		}
	}
	
	//Convinience methods, why wouldn't java allow operator overloading is beyond me.
	private Vector3f vadd(Vector3f a, Vector3f b)
	{
		Vector3f out = new Vector3f(a);
		out.add(b);
		//Vector3f.add(a, b, out);
		return out;
	}
	
	private Vector3f vsub(Vector3f a, Vector3f b)
	{
		Vector3f out = new Vector3f(a);
		out.sub(b);
		//Vector3f.sub(a, b, out);
		return out;
	}
	
	private Vector3f smult(Vector3f in, float scale)
	{
		Vector3f out = new Vector3f(in);
		out.mul(scale);
		return out;
	}

	Vector3f corners[] = new Vector3f[8];
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#isBoxInFrustrum(io.xol.chunkstories.api.math.Vector3f, io.xol.chunkstories.api.math.Vector3f)
	 */
	@Override
	public boolean isBoxInFrustrum(Vector3fc center, Vector3fc dimensions)
	{
		//Manual loop unrolling
		/*for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				for(int k = 0; k < 2; k++)
				{
					corners[i * 4 + j * 2 + k].x = center.x + dimensions.x / 2f * (i == 0 ? -1 : 1);
					corners[i * 4 + j * 2 + k].y = center.y + dimensions.y / 2f * (j == 0 ? -1 : 1);
					corners[i * 4 + j * 2 + k].z = center.z + dimensions.z / 2f * (k == 0 ? -1 : 1);
				}
			}
		}*/
		
		//dimensions.scale(0.5f);
		
		final float PLUSONE = 0.5f;
		final float MINUSONE = -0.5f;
		
		//i=0 j=0 k=0
		corners[0].x = (center.x() + dimensions.x()   * MINUSONE);
		corners[0].y = (center.y() + dimensions.y()   * MINUSONE);
		corners[0].z = (center.z() + dimensions.z()   * MINUSONE);
		//i=0 j=0 k=1
		corners[1].x = (center.x() + dimensions.x()   * MINUSONE);
		corners[1].y = (center.y() + dimensions.y()   * MINUSONE);
		corners[1].z = (center.z() + dimensions.z()   *  PLUSONE);
		//i=0 j=1 k=0
		corners[2].x = (center.x() + dimensions.x()   * MINUSONE);
		corners[2].y = (center.y() + dimensions.y()   *  PLUSONE);
		corners[2].z = (center.z() + dimensions.z()   * MINUSONE);
		//i=0 j=1 k=1
		corners[3].x = (center.x() + dimensions.x()   * MINUSONE);
		corners[3].y = (center.y() + dimensions.y()   *  PLUSONE);
		corners[3].z = (center.z() + dimensions.z()   *  PLUSONE);
		//i=1 j=0 k=0
		corners[4].x = (center.x() + dimensions.x()   *  PLUSONE);
		corners[4].y = (center.y() + dimensions.y()   * MINUSONE);
		corners[4].z = (center.z() + dimensions.z()   * MINUSONE);
		//i=1 j=0 k=1
		corners[5].x = (center.x() + dimensions.x()   *  PLUSONE);
		corners[5].y = (center.y() + dimensions.y()   * MINUSONE);
		corners[5].z = (center.z() + dimensions.z()   *  PLUSONE);
		//i=1 j=1 k=0
		corners[6].x = (center.x() + dimensions.x()   *  PLUSONE);
		corners[6].y = (center.y() + dimensions.y()   *  PLUSONE);
		corners[6].z = (center.z() + dimensions.z()   * MINUSONE);
		//i=1 j=1 k=1
		corners[7].x = (center.x() + dimensions.x()   *  PLUSONE);
		corners[7].y = (center.y() + dimensions.y()   *  PLUSONE);
		corners[7].z = (center.z() + dimensions.z()   *  PLUSONE);
		
		for(int i = 0; i < 6; i++)
		{
			int out = 0;
			int in = 0;
			for(int c = 0; c < 8; c++)
			{
				//System.out.println(i+" "+c+" "+cameraPlanes[i].distance(corners[c]) + " center "+center);
				if(cameraPlanes[i].distance(corners[c]) < 0)
					out++;
				else
					in++;
			}
			if(in == 0)
			{
				//System.out.println("Rejected "+center+" on plane "+i);
				return false;
			}
			else if(out > 0)
			{
				// Partially occluded.
				//return false;
			}
		}
		return true;
	}

	private double toRad(double d)
	{
		return d / 360 * 2 * Math.PI;
	}

	private void translateCamera()
	{
		untranslatedMVP4f.set(modelViewMatrix4f);
		
		untranslatedMVP4f.invert(untranslatedMVP4fInv);
		//Matrix4f.invert(untranslatedMVP4f, untranslatedMVP4fInv);

		modelViewMatrix4f.translate(new Vector3f((float)this.position.x, (float)this.position.y, (float)this.position.z).negate());
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#setupShader(io.xol.engine.graphics.shaders.ShaderProgram)
	 */
	@Override
	public void setupShader(Shader shader)
	{
		// Helper function to clean code from messy bits :)
		shader.setUniformMatrix4f("projectionMatrix", projectionMatrix4f);
		shader.setUniformMatrix4f("projectionMatrixInv", projectionMatrix4fInverted);

		shader.setUniformMatrix4f("modelViewMatrix", modelViewMatrix4f);
		shader.setUniformMatrix4f("modelViewMatrixInv", modelViewMatrix4fInverted);

		shader.setUniformMatrix3f("normalMatrix", normalMatrix3f);
		shader.setUniformMatrix3f("normalMatrixInv", normalMatrix3fInverted);
		
		shader.setUniformMatrix4f("modelViewProjectionMatrix", modelViewProjectionMatrix4f);
		shader.setUniformMatrix4f("modelViewProjectionMatrixInv", modelViewProjectionMatrix4fInverted);
		
		shader.setUniformMatrix4f("untranslatedMV", untranslatedMVP4f);
		shader.setUniformMatrix4f("untranslatedMVInv", untranslatedMVP4fInv);
		
		shader.setUniform2f("screenViewportSize", this.viewportWidth, this.viewportHeight);

		shader.setUniform3f("camPos", getCameraPosition());
		shader.setUniform3f("camUp", up);

		WorldRenderer wr = Client.getInstance().getRenderingInterface().getWorldRenderer();
		if(wr != null) {
			WorldRendererImplementation wri = (WorldRendererImplementation) wr;
			float am = wri.averageLuma.getApertureModifier();

			shader.setUniform1f("apertureModifier", am);
			//System.out.println(am);
		}
		else 
			shader.setUniform1f("apertureModifier", 1.0f);
	}
	
	public Vector3f transform3DCoordinate(Vector3fc in)
	{
		return transform3DCoordinate(new Vector4f(in.x(), in.y(), in.z(), 1f));
	}
	
	/**
	 * Spits out where some point in world coordinates ends up on the screen
	 * @param in
	 * @return
	 */
	public Vector3f transform3DCoordinate(Vector4fc in)
	{
		//position = new Vector4f(-(float)e.posX, -(float)e.posY, -(float)e.posZ, 1f);
		//position = new Vector4f(1f, 1f, 1f, 1);
		Matrix4f mvm = this.modelViewMatrix4f;
		Matrix4f pm = this.projectionMatrix4f;

		//Matrix4f combined = Matrix4f.mul(pm, mvm, null);
		
		Vector4f transormed = new Vector4f();
		mvm.transform(in, transormed);
		
		pm.transform(transormed, transormed);
		Vector3f posOnScreen = new Vector3f((float)in.x(), (float)in.y(), 0f);
		float scale = 1/in.z();
		posOnScreen.mul(scale);

		posOnScreen.x = ((posOnScreen.x() * 0.5f + 0.5f) * viewportWidth);
		posOnScreen.y = (((posOnScreen.y() * 0.5f + 0.5f)) * viewportHeight);
		posOnScreen.z = (scale);
		return posOnScreen;
	}

	@Override
	public Vector3f getViewDirection()
	{
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		return new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
	}

	@Override
	public Vector3dc getCameraPosition()
	{
		return this.position;
	}

	@Override
	public void setCameraPosition(Vector3dc pos)
	{
		this.position.x = pos.x();
		this.position.y = pos.y();
		this.position.z = pos.z();
	}

	@Override
	public boolean isBoxInFrustrum(CollisionBox box)
	{
		//TODO don't create fucking objects
		return this.isBoxInFrustrum(new Vector3f((float)(box.xpos + box.xw / 2),(float)( box.ypos + box.h / 2),(float)( box.zpos + box.zw / 2)), new Vector3f((float)box.xw, (float)box.h, (float)box.zw));
	}

	@Override
	public float getFOV()
	{
		return fov;
	}

	@Override
	public void setFOV(float fov)
	{
		this.fov = fov;
	}

	@Override
	public float getRotationX()
	{
		return rotationX;
	}

	@Override
	public void setRotationX(float rotationX)
	{
		this.rotationX = rotationX;
	}

	@Override
	public float getRotationY()
	{
		return rotationY;
	}

	@Override
	public void setRotationY(float rotationY)
	{
		this.rotationY = rotationY;
	}

	@Override
	public float getRotationZ()
	{
		return rotationZ;
	}

	@Override
	public void setRotationZ(float rotationZ)
	{
		this.rotationZ = rotationZ;
	}
}
