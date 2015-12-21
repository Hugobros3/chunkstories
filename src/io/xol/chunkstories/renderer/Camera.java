package io.xol.chunkstories.renderer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.FloatBuffer;

import io.xol.chunkstories.client.Client;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.shaders.ShaderProgram;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Camera
{
	public float view_rotx = 30.0f;
	public float view_roty = 30.0f;
	public float view_rotz = 0f;

	public float camPosX = 10;
	public float camPosY = -75;
	public float camPosZ = -18;

	float lastPX = -1f;
	float lastPY = -1f;

	public FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
	public FloatBuffer projectionMatrixInverse = BufferUtils.createFloatBuffer(16);

	public FloatBuffer modelViewMatrix = BufferUtils.createFloatBuffer(16);
	public FloatBuffer modelViewMatrixInverse = BufferUtils.createFloatBuffer(16);

	public FloatBuffer normalMatrix = BufferUtils.createFloatBuffer(9);
	public FloatBuffer normalMatrixInverse = BufferUtils.createFloatBuffer(9);

	public Matrix4f projectionMatrix4f = new Matrix4f();
	public Matrix4f projectionMatrix4fInverted = new Matrix4f();

	public Matrix4f modelViewMatrix4f = new Matrix4f();
	public Matrix4f modelViewMatrix4fInverted = new Matrix4f();

	public Matrix3f normalMatrix3f = new Matrix3f();
	public Matrix3f normalMatrix3fInverted = new Matrix3f();

	private FloatBuffer getFloatBuffer(float[] f)
	{
		FloatBuffer buf = BufferUtils.createFloatBuffer(f.length).put(f);
		buf.flip();
		return buf;
	}

	public void alUpdate()
	{
		// Sound update
		float a = (float) ((180 - view_rotx) / 360f * 2 * Math.PI);
		float b = 0;// (float) ((view_roty)/360f*2*Math.PI);
		FloatBuffer listenerOrientation = getFloatBuffer(new float[] { (float) Math.sin(a) * 1 * (float) Math.cos(b), (float) Math.sin(b) * 1, (float) Math.cos(a) * 1 * (float) Math.cos(b), 0.0f, 1.0f, 0.0f });
		Client.getSoundManager().setListenerPosition(-camPosX, -camPosY, -camPosZ, listenerOrientation);
	}

	public float fov = 45;

	public void updateMatricesForShaderUniforms()
	{
		projectionMatrixInverse.position(0);
		projectionMatrix.position(0);
		
		projectionMatrix4f.store(projectionMatrix);
		
		// projectionMatrix.position(0);
		// projectionMatrix4f.store(projectionMatrix);
		Matrix4f.invert(projectionMatrix4f, projectionMatrix4fInverted);
		// Matrix4f invertedProjection = (Matrix4f) projectionMatrix4f.invert();
		projectionMatrix4fInverted.store(projectionMatrixInverse);
		
		// Allow for read in shader
		modelViewMatrix.position(0);
		modelViewMatrix4f.store(modelViewMatrix);
		
		Matrix4f.invert(modelViewMatrix4f, modelViewMatrix4fInverted);
		modelViewMatrixInverse.position(0);
		modelViewMatrix4fInverted.store(modelViewMatrixInverse);

		Matrix4f tempMatrix = new Matrix4f();
		
		Matrix4f.invert(modelViewMatrix4f, tempMatrix);
		Matrix4f.transpose(tempMatrix, tempMatrix);

		normalMatrix3f.m00 = tempMatrix.m00;
		normalMatrix3f.m01 = tempMatrix.m01;
		normalMatrix3f.m02 = tempMatrix.m02;

		normalMatrix3f.m10 = tempMatrix.m10;
		normalMatrix3f.m11 = tempMatrix.m11;
		normalMatrix3f.m12 = tempMatrix.m12;

		normalMatrix3f.m20 = tempMatrix.m20;
		normalMatrix3f.m21 = tempMatrix.m21;
		normalMatrix3f.m22 = tempMatrix.m22;

		normalMatrix.clear();
		normalMatrix3f.store(normalMatrix);
		Matrix3f.invert(normalMatrix3f, normalMatrix3fInverted);
		normalMatrixInverse.clear();
		normalMatrix3fInverted.store(normalMatrixInverse);
		// projectionMatrix.position(0);
		// projectionMatrixInverse.position(0);
	}

	public void justSetup()
	{
		// Frustrum values
		float fovRad = (float) toRad(fov);

		float aspect = (float) XolioWindow.frameW / (float) XolioWindow.frameH;
		float top = (float) Math.tan(fovRad) * 0.1f;
		float bottom = -top;
		float left = aspect * bottom;
		float right = aspect * top;
		float near = 0.1f;
		float far = 3000f;
		// Generate the projection matrix
		
		projectionMatrix4f.setIdentity();
		projectionMatrix4f.m00 = (near * 2) / (right - left);
		projectionMatrix4f.m11 = (near * 2) / (top - bottom);
		float A = (right + left) / (right - left);
		float B = (top + bottom) / ( top - bottom);
		float C = - (far + near) / (far - near);
		float D = - (2 * far * near) / (far - near);
		projectionMatrix4f.m20 = A;
		projectionMatrix4f.m21 = B;
		projectionMatrix4f.m22 = C;
		projectionMatrix4f.m32 = D;
		projectionMatrix4f.m23 = -1;
		projectionMatrix4f.m33 = 0;
		//System.out.println(projectionMatrix4f);
		
		// Grab the generated matrix
		
		modelViewMatrix4f.setIdentity();
		// Rotate the modelview matrix
		modelViewMatrix4f.rotate((float) (view_rotx / 180 * Math.PI), new Vector3f( 1.0f, 0.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (view_roty / 180 * Math.PI), new Vector3f( 0.0f, 1.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (view_rotz / 180 * Math.PI), new Vector3f( 0.0f, 0.0f, 1.0f));
	
		updateMatricesForShaderUniforms();
	}

	private double toRad(double d)
	{
		return d / 360 * 2 * Math.PI;
	}

	public void translate()
	{
		modelViewMatrix4f.translate(new Vector3f(camPosX, camPosY, camPosZ));
		//glTranslatef(camPosX, camPosY, camPosZ);
		updateMatricesForShaderUniforms();
	}

	public void setupShader(ShaderProgram shaderProgram)
	{
		// Helper function to clean code from messy bits :)
		shaderProgram.setUniformMatrix4f("projectionMatrix", projectionMatrix);
		shaderProgram.setUniformMatrix4f("projectionMatrixInv", projectionMatrixInverse);

		shaderProgram.setUniformMatrix4f("modelViewMatrix", modelViewMatrix);
		shaderProgram.setUniformMatrix4f("modelViewMatrixInv", modelViewMatrixInverse);

		shaderProgram.setUniformMatrix3f("normalMatrix", normalMatrix);
		shaderProgram.setUniformMatrix3f("normalMatrixInv", normalMatrixInverse);
	}
	
	public Vector3f transform3DCoordinate(Vector3f in)
	{
		return transform3DCoordinate(new Vector4f(in.x, in.y, in.z, 1f));
	}
	
	public Vector3f transform3DCoordinate(Vector4f in)
	{
		//position = new Vector4f(-(float)e.posX, -(float)e.posY, -(float)e.posZ, 1f);
		//position = new Vector4f(1f, 1f, 1f, 1);
		Matrix4f mvm = this.modelViewMatrix4f;
		Matrix4f pm = this.projectionMatrix4f;

		//Matrix4f combined = Matrix4f.mul(pm, mvm, null);
		
		in = Matrix4f.transform(mvm, in, null);
		// transformed.scale(1/transformed.w);
		in = Matrix4f.transform(pm, in, null);
		
		//in = Matrix4f.transform(combined, in, null);

		//position.scale(1/position.w);

		Vector3f posOnScreen = new Vector3f(in.x, in.y, 0f);
		float scale = 1/in.z;
		posOnScreen.scale(scale);

		posOnScreen.x = (posOnScreen.x * 0.5f + 0.5f) * XolioWindow.frameW;
		posOnScreen.y = ((posOnScreen.y * 0.5f + 0.5f)) * XolioWindow.frameH;
		posOnScreen.z = scale;
		return posOnScreen;
	}
}
