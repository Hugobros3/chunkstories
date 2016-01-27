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

	public FloatBuffer modelViewProjectionMatrix = BufferUtils.createFloatBuffer(16);
	public FloatBuffer modelViewProjectionMatrixInverse = BufferUtils.createFloatBuffer(16);
	
	public Matrix4f projectionMatrix4f = new Matrix4f();
	public Matrix4f projectionMatrix4fInverted = new Matrix4f();

	public Matrix4f modelViewProjectionMatrix4f = new Matrix4f();
	public Matrix4f modelViewProjectionMatrix4fInverted = new Matrix4f();
	
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

	public void alUpdate()
	{
		float rotH = view_roty;
		float rotV = view_rotx;
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
		Matrix4f.mul(projectionMatrix4f, modelViewMatrix4f, modelViewProjectionMatrix4f);
		Matrix4f.invert(modelViewProjectionMatrix4f, modelViewMatrix4fInverted);
		modelViewProjectionMatrix.clear();
		modelViewProjectionMatrix4f.store(modelViewProjectionMatrix);
		modelViewProjectionMatrixInverse.clear();
		modelViewProjectionMatrix4fInverted.store(modelViewProjectionMatrixInverse);
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
		
		Vector3f position = new Vector3f(-camPosX, -camPosY, -camPosZ);
		
		float rotH = view_roty;
		float rotV = view_rotx;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3f lookAt = new Vector3f((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f.cross(lookAt, up, up);
		Vector3f.cross(up, lookAt, up);
		
		Vector3f.add(position, lookAt, lookAt);
		//modelViewMatrix4f = MatrixHelper.getLookAtMatrix(position, lookAt, up);
		
		//System.out.println(modelViewMatrix4f);
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
		float ratio = (float) XolioWindow.frameW / (float) XolioWindow.frameH;
		float nh = 0.1f * tang;
		float nw = nh * ratio;
		float fh = 3000f  * tang;
		float fw = fh * ratio;
		
		// Recreate the 3 vectors for the algorithm
		Vector3f position = new Vector3f(-camPosX, -camPosY, -camPosZ);
		
		float rotH = view_roty;
		float rotV = view_rotx;
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
		/*
		// compute the 4 corners of the frustum on the near plane
		ntl = nc + Y * nh - X * nw;
		ntr = nc + Y * nh + X * nw;
		nbl = nc - Y * nh - X * nw;
		nbr = nc - Y * nh + X * nw;
	
		// compute the 4 corners of the frustum on the far plane
		ftl = fc + Y * fh - X * fw;
		ftr = fc + Y * fh + X * fw;
		fbl = fc - Y * fh - X * fw;
		fbr = fc - Y * fh + X * fw;
		
		pl[TOP].set3Points(ntr,ntl,ftl);
		pl[BOTTOM].set3Points(nbl,nbr,fbr);
		pl[LEFT].set3Points(ntl,nbl,fbl);
		pl[RIGHT].set3Points(nbr,ntr,fbr);
		pl[NEARP].set3Points(ntl,ntr,nbr);
		pl[FARP].set3Points(ftr,ftl,fbl);
		 */
		
		cameraPlanes[0].setup(nearTopRight, nearTopLeft, farTopLeft);
		cameraPlanes[1].setup(nearBottomLeft, nearBottomRight, farBottomRight);
		cameraPlanes[2].setup(nearTopLeft, nearBottomLeft, farBottomLeft);
		cameraPlanes[3].setup(nearBottomRight, nearTopRight, farBottomRight);
		cameraPlanes[4].setup(nearTopLeft, nearTopRight, nearBottomRight);
		cameraPlanes[5].setup(farTopRight, farTopLeft, farBottomLeft);
	}
	
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
	
	public boolean isBoxInFrustrum(Vector3f center, Vector3f dimensions)
	{
		Vector3f corners[] = new Vector3f[8];
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				for(int k = 0; k < 2; k++)
				{
					Vector3f corner = new Vector3f();
					corner.x = center.x + dimensions.x / 2f * (i == 0 ? -1 : 1);
					corner.y = center.y + dimensions.y / 2f * (j == 0 ? -1 : 1);
					corner.z = center.z + dimensions.z / 2f * (k == 0 ? -1 : 1);
					corners[i * 4 + j * 2 + k] = corner;
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
		modelViewMatrix4f.translate(new Vector3f(camPosX, camPosY, camPosZ));
		
		//glTranslatef(camPosX, camPosY, camPosZ);
		computeFrustrumPlanes();
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
		
		shaderProgram.setUniformMatrix4f("modelViewProjectionMatrix", modelViewProjectionMatrix);
		shaderProgram.setUniformMatrix4f("modelViewProjectionMatrixInv", modelViewProjectionMatrixInverse);
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
