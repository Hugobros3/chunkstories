package io.xol.chunkstories.renderer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.FloatBuffer;

import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.shaders.ShaderProgram;

import org.lwjgl.BufferUtils;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

public class Camera
{
	//Viewport size
	public int viewportWidth, viewportHeight;
	
	//Camera rotations
	public float rotationX = 0.0f;
	public float rotationY = 0.0f;
	public float rotationZ = 0.0f;

	//Camera positions

	public Vector3d pos = new Vector3d();
	
	//public double camPosX = 10;
	//public double camPosY = -75;
	//public double camPosZ = -18;

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

	public Camera()
	{
		// Init frustrum planes
		for(int i = 0; i < 6; i++)
		{
			cameraPlanes[i] = new FrustrumPlane();
		}
	}
	
	private FloatBuffer getFloatBuffer(float[] f)
	{
		FloatBuffer buf = BufferUtils.createFloatBuffer(f.length).put(f);
		buf.flip();
		return buf;
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
		Vector3f.cross(lookAt, up, up);
		Vector3f.cross(up, lookAt, up);
		
		FloatBuffer listenerOrientation = getFloatBuffer(new float[]{
				lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z
		});
		//FloatBuffer listenerOrientation = getFloatBuffer(new float[] { (float) Math.sin(a) * 1 * (float) Math.cos(b), (float) Math.sin(b) * 1, (float) Math.cos(a) * 1 * (float) Math.cos(b), 0.0f, 1.0f, 0.0f });
		Client.getInstance().getSoundManager().setListenerPosition(-pos.x, -pos.y, -pos.z, listenerOrientation);
	}

	public float fov = 45;

	/**
	 * Computes inverted and derived matrices
	 */
	public void updateMatricesForShaderUniforms()
	{
		//Invert two main patrices
		Matrix4f.invert(projectionMatrix4f, projectionMatrix4fInverted);
		Matrix4f.invert(modelViewMatrix4f, modelViewMatrix4fInverted);
		
		//Build normal matrix
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
		//Invert it
		Matrix3f.invert(normalMatrix3f, normalMatrix3fInverted);
		
		//Premultiplied versions ( optimization for poor drivers that don't figure it out themselves )
		Matrix4f.mul(projectionMatrix4f, modelViewMatrix4f, modelViewProjectionMatrix4f);
		Matrix4f.invert(modelViewProjectionMatrix4f, modelViewProjectionMatrix4fInverted);
	}

	public void justSetup(int width, int height)
	{
		this.viewportWidth = width;
		this.viewportHeight = height;
		// Frustrum values
		float fovRad = (float) toRad(fov);

		float aspect = (float) width / (float) height;
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
		
		// Grab the generated matrix
		
		modelViewMatrix4f.setIdentity();
		// Rotate the modelview matrix
		modelViewMatrix4f.rotate((float) (rotationX / 180 * Math.PI), new Vector3f( 1.0f, 0.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (rotationY / 180 * Math.PI), new Vector3f( 0.0f, 1.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (rotationZ / 180 * Math.PI), new Vector3f( 0.0f, 0.0f, 1.0f));
		
		Vector3f position = pos.castToSP();
		position = position.negate(position);
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f.cross(lookAt, up, up);
		Vector3f.cross(up, lookAt, up);
		
		Vector3f.add(position, lookAt, lookAt);
		
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
	}

	FrustrumPlane[] cameraPlanes = new FrustrumPlane[6];
	
	public class FrustrumPlane {
		float A, B, C, D;
		Vector3f n;
		
		public void setup(Vector3f p1, Vector3f p2, Vector3f p3)
		{
			Vector3f v = new Vector3f();
			Vector3f u = new Vector3f();
			Vector3f.sub(p2, p1, v);
			Vector3f.sub(p3, p1, u);
			n = new Vector3f();
			Vector3f.cross(v, u, n);
			n.normalise();
			A = n.x;
			B = n.y;
			C = n.z;
			//n.negate();
			D = -Vector3f.dot(p1, n);
		}
		
		public float distance(Vector3f point)
		{
			return A * point.x + B * point.y + C * point.z + D;
		}
	}
	
	private void computeFrustrumPlanes()
	{
		Vector3f temp = new Vector3f();
		//Init values
		float tang = (float)Math.tan(toRad(fov)) ;
		float ratio = (float) viewportWidth / (float) viewportHeight;
		float nh = 0.1f * tang;
		float nw = nh * ratio;
		float fh = 3000f  * tang;
		float fw = fh * ratio;
		
		// Recreate the 3 vectors for the algorithm

		Vector3f position = pos.castToSP();
		position = position.negate(position);
		//Vector3f position = new Vector3f((float)-camPosX, (float)-camPosY, (float)-camPosZ);
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f.cross(lookAt, up, up);
		Vector3f.cross(up, lookAt, up);
		
		Vector3f.add(position, lookAt, lookAt);
		
		// Create the 6 frustrum planes
		Vector3f Z = new Vector3f();
		Vector3f.sub(position, lookAt, Z);
		Z.normalise();
		
		Vector3f X = new Vector3f();
		Vector3f.cross(up, Z, X);
		X.normalise();

		Vector3f Y = new Vector3f();
		Vector3f.cross(Z, X, Y);
		
		Vector3f nearCenterPoint = new Vector3f();
		temp = new Vector3f(Z);
		temp.scale(0.1f);
		Vector3f.sub(position, temp, nearCenterPoint);

		Vector3f farCenterPoint = new Vector3f();
		temp = new Vector3f(Z);
		temp.scale(3000f);
		Vector3f.sub(position, temp, farCenterPoint);
		
		// Eventually the fucking points
		Vector3f nearTopLeft = vadd(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));
		Vector3f nearTopRight = vadd(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3f nearBottomLeft = vsub(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3f nearBottomRight = vsub(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));

		Vector3f farTopLeft = vadd(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		Vector3f farTopRight = vadd(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3f farBottomLeft = vsub(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3f farBottomRight = vsub(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		
		cameraPlanes[0].setup(nearTopRight, nearTopLeft, farTopLeft);
		cameraPlanes[1].setup(nearBottomLeft, nearBottomRight, farBottomRight);
		cameraPlanes[2].setup(nearTopLeft, nearBottomLeft, farBottomLeft);
		cameraPlanes[3].setup(nearBottomRight, nearTopRight, farBottomRight);
		cameraPlanes[4].setup(nearTopLeft, nearTopRight, nearBottomRight);
		cameraPlanes[5].setup(farTopRight, farTopLeft, farBottomLeft);
		
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
		Vector3f out = new Vector3f();
		Vector3f.add(a, b, out);
		return out;
	}
	
	private Vector3f vsub(Vector3f a, Vector3f b)
	{
		Vector3f out = new Vector3f();
		Vector3f.sub(a, b, out);
		return out;
	}
	
	private Vector3f smult(Vector3f in, float scale)
	{
		Vector3f out = new Vector3f(in);
		out.scale(scale);
		return out;
	}

	Vector3f corners[] = new Vector3f[8];
	
	public boolean isBoxInFrustrum(Vector3f center, Vector3f dimensions)
	{
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				for(int k = 0; k < 2; k++)
				{
					//Vector3f corner = new Vector3f();
					corners[i * 4 + j * 2 + k].x = center.x + dimensions.x / 2f * (i == 0 ? -1 : 1);
					corners[i * 4 + j * 2 + k].y = center.y + dimensions.y / 2f * (j == 0 ? -1 : 1);
					corners[i * 4 + j * 2 + k].z = center.z + dimensions.z / 2f * (k == 0 ? -1 : 1);
					//corners[i * 4 + j * 2 + k] = corner;
				}
			}
		}
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

	public void translate()
	{
		untranslatedMVP4f.load(modelViewMatrix4f);
		Matrix4f.invert(untranslatedMVP4f, untranslatedMVP4fInv);

		modelViewMatrix4f.translate(pos.castToSP());
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
	}

	/**
	 * Sends the common matrices ( projection, modelview etc) to the shaderProgram
	 * @param shaderProgram
	 */
	public void setupShader(ShaderProgram shaderProgram)
	{
		// Helper function to clean code from messy bits :)
		shaderProgram.setUniformMatrix4f("projectionMatrix", projectionMatrix4f);
		shaderProgram.setUniformMatrix4f("projectionMatrixInv", projectionMatrix4fInverted);

		shaderProgram.setUniformMatrix4f("modelViewMatrix", modelViewMatrix4f);
		shaderProgram.setUniformMatrix4f("modelViewMatrixInv", modelViewMatrix4fInverted);

		shaderProgram.setUniformMatrix3f("normalMatrix", normalMatrix3f);
		shaderProgram.setUniformMatrix3f("normalMatrixInv", normalMatrix3fInverted);
		
		shaderProgram.setUniformMatrix4f("modelViewProjectionMatrix", modelViewProjectionMatrix4f);
		shaderProgram.setUniformMatrix4f("modelViewProjectionMatrixInv", modelViewProjectionMatrix4fInverted);
		
		shaderProgram.setUniformMatrix4f("untranslatedMVP", untranslatedMVP4f);
		shaderProgram.setUniformMatrix4f("untranslatedMVPInv", untranslatedMVP4fInv);
		
		shaderProgram.setUniformFloat2("screenViewportSize", this.viewportWidth, this.viewportHeight);

		shaderProgram.setUniformFloat3("camPos", pos.clone().negate());
	}
	
	public Vector3f transform3DCoordinate(Vector3f in)
	{
		return transform3DCoordinate(new Vector4f(in.x, in.y, in.z, 1f));
	}
	
	/**
	 * Spits out where some point in world coordinates ends up on the screen
	 * @param in
	 * @return
	 */
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

		posOnScreen.x = (posOnScreen.x * 0.5f + 0.5f) * viewportWidth;
		posOnScreen.y = ((posOnScreen.y * 0.5f + 0.5f)) * viewportHeight;
		posOnScreen.z = scale;
		return posOnScreen;
	}

	public Vector3f getViewDirection()
	{
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		return new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
	}
}
