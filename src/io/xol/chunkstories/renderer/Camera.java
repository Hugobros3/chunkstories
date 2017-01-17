package io.xol.chunkstories.renderer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.nio.FloatBuffer;

import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.client.Client;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector3m;
import io.xol.engine.math.lalgb.vector.operations.VectorCrossProduct;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

public class Camera implements CameraInterface
{
	//Viewport size
	public int viewportWidth, viewportHeight;
	
	//Camera rotations
	public float rotationX = 0.0f;
	public float rotationY = 0.0f;
	public float rotationZ = 0.0f;
	//Camera positions
	private Vector3dm pos = new Vector3dm();
	
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
		Vector3fm lookAt = new Vector3fm((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3fm up = new Vector3fm(0.0f, 1.0f, 0.0f);
		VectorCrossProduct.cross33(lookAt, up, up);
		VectorCrossProduct.cross33(up, lookAt, up);
		
		FloatBuffer listenerOrientation = getFloatBuffer(new float[]{
				lookAt.getX(), lookAt.getY(), lookAt.getZ(), up.getX(), up.getY(), up.getZ()
		});
		//FloatBuffer listenerOrientation = getFloatBuffer(new float[] { (float) Math.sin(a) * 1 * (float) Math.cos(b), (float) Math.sin(b) * 1, (float) Math.cos(a) * 1 * (float) Math.cos(b), 0.0f, 1.0f, 0.0f });
		Client.getInstance().getSoundManager().setListenerPosition(-pos.getX(), -pos.getY(), -pos.getZ(), listenerOrientation);
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
		modelViewMatrix4f.rotate((float) (rotationZ / 180 * Math.PI), new Vector3fm( 0.0f, 0.0f, 1.0f));
		modelViewMatrix4f.rotate((float) (rotationX / 180 * Math.PI), new Vector3fm( 1.0f, 0.0f, 0.0f));
		modelViewMatrix4f.rotate((float) (rotationY / 180 * Math.PI), new Vector3fm( 0.0f, 1.0f, 0.0f));
		
		Vector3m<Float> position = pos.castToSinglePrecision();
		position = position.negate();
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3fm lookAt = new Vector3fm((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		//Vector3fm direction = new Vector3fm((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3fm up = new Vector3fm(0.0f, 1.0f, 0.0f);
		VectorCrossProduct.cross33(lookAt, up, up);
		VectorCrossProduct.cross33(up, lookAt, up);
		
		lookAt.add(position);
		//Vector3fm.add(position, lookAt, lookAt);
		position.scale(0.0f);
	    
	   // modelViewMatrix4f = MatrixHelper.getLookAtMatrix(position, direction, up);
	    
	    //return result;
		
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
	}

	FrustrumPlane[] cameraPlanes = new FrustrumPlane[6];
	
	public class FrustrumPlane {
		float A, B, C, D;
		Vector3fm n;
		
		public void setup(Vector3fm p1, Vector3fm p2, Vector3fm p3)
		{
			Vector3fm v = new Vector3fm(p2);
			Vector3fm u = new Vector3fm(p3);
			
			v.sub(p1);
			//Vector3fm.sub(p2, p1, v);
			u.sub(p1);
			//Vector3fm.sub(p3, p1, u);
			n = new Vector3fm();
			VectorCrossProduct.cross33(v, u, n);
			n.normalize();
			A = n.getX();
			B = n.getY();
			C = n.getZ();
			//n.negate();
			D = -p1.dot(n);
		}
		
		public float distance(Vector3fm point)
		{
			return A * point.getX() + B * point.getY() + C * point.getZ() + D;
		}
	}
	
	private void computeFrustrumPlanes()
	{
		Vector3fm temp = new Vector3fm();
		//Init values
		float tang = (float)Math.tan(toRad(fov)) ;
		float ratio = (float) viewportWidth / (float) viewportHeight;
		float nh = 0.1f * tang;
		float nw = nh * ratio;
		float fh = 3000f  * tang;
		float fw = fh * ratio;
		
		// Recreate the 3 vectors for the algorithm

		Vector3m<Float> position = pos.castToSinglePrecision();
		position = position.negate();
		//Vector3fm position = new Vector3fm((float)-camPosX, (float)-camPosY, (float)-camPosZ);
		
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		Vector3fm lookAt = new Vector3fm((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
		
		Vector3fm up = new Vector3fm(0.0f, 1.0f, 0.0f);
		VectorCrossProduct.cross33(lookAt, up, up);
		VectorCrossProduct.cross33(up, lookAt, up);
		
		lookAt.add(position);
		//Vector3fm.add(position, lookAt, lookAt);
		
		// Create the 6 frustrum planes
		Vector3fm Z = new Vector3fm(position);
		
		Z.sub(lookAt);
		//Vector3fm.sub(position, lookAt, Z);
		Z.normalize();
		
		Vector3fm X = new Vector3fm();
		VectorCrossProduct.cross33(up, Z, X);
		X.normalize();

		Vector3fm Y = new Vector3fm();
		VectorCrossProduct.cross33(Z, X, Y);
		
		Vector3fm nearCenterPoint = new Vector3fm(position);
		temp = new Vector3fm(Z);
		temp.scale(0.1f);
		
		nearCenterPoint.sub(temp);
		//Vector3fm.sub(position, temp, nearCenterPoint);

		Vector3fm farCenterPoint = new Vector3fm(position);
		temp = new Vector3fm(Z);
		temp.scale(3000f);
		
		farCenterPoint.sub(temp);
		//Vector3fm.sub(position, temp, farCenterPoint);
		
		// Eventually the fucking points
		Vector3fm nearTopLeft = vadd(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));
		Vector3fm nearTopRight = vadd(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3fm nearBottomLeft = vsub(nearCenterPoint, vadd(smult(Y, nh), smult(X, nw)));
		Vector3fm nearBottomRight = vsub(nearCenterPoint, vsub(smult(Y, nh), smult(X, nw)));

		Vector3fm farTopLeft = vadd(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		Vector3fm farTopRight = vadd(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3fm farBottomLeft = vsub(farCenterPoint, vadd(smult(Y, fh), smult(X, fw)));
		Vector3fm farBottomRight = vsub(farCenterPoint, vsub(smult(Y, fh), smult(X, fw)));
		
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
					corners[i * 4 + j * 2 + k] = new Vector3fm();
				}
			}
		}
	}
	
	//Convinience methods, why wouldn't java allow operator overloading is beyond me.
	private Vector3fm vadd(Vector3fm a, Vector3fm b)
	{
		Vector3fm out = new Vector3fm(a);
		out.add(b);
		//Vector3fm.add(a, b, out);
		return out;
	}
	
	private Vector3fm vsub(Vector3fm a, Vector3fm b)
	{
		Vector3fm out = new Vector3fm(a);
		out.sub(b);
		//Vector3fm.sub(a, b, out);
		return out;
	}
	
	private Vector3fm smult(Vector3fm in, float scale)
	{
		Vector3fm out = new Vector3fm(in);
		out.scale(scale);
		return out;
	}

	Vector3fm corners[] = new Vector3fm[8];
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#isBoxInFrustrum(io.xol.engine.math.lalgb.Vector3fm, io.xol.engine.math.lalgb.Vector3fm)
	 */
	@Override
	public boolean isBoxInFrustrum(Vector3<Float> center, Vector3<Float> dimensions)
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
		corners[0].setX(center.getX() + dimensions.getX()   * MINUSONE);
		corners[0].setY(center.getY() + dimensions.getY()   * MINUSONE);
		corners[0].setZ(center.getZ() + dimensions.getZ()   * MINUSONE);
		//i=0 j=0 k=1
		corners[1].setX(center.getX() + dimensions.getX()   * MINUSONE);
		corners[1].setY(center.getY() + dimensions.getY()   * MINUSONE);
		corners[1].setZ(center.getZ() + dimensions.getZ()   *  PLUSONE);
		//i=0 j=1 k=0
		corners[2].setX(center.getX() + dimensions.getX()   * MINUSONE);
		corners[2].setY(center.getY() + dimensions.getY()   *  PLUSONE);
		corners[2].setZ(center.getZ() + dimensions.getZ()   * MINUSONE);
		//i=0 j=1 k=1
		corners[3].setX(center.getX() + dimensions.getX()   * MINUSONE);
		corners[3].setY(center.getY() + dimensions.getY()   *  PLUSONE);
		corners[3].setZ(center.getZ() + dimensions.getZ()   *  PLUSONE);
		//i=1 j=0 k=0
		corners[4].setX(center.getX() + dimensions.getX()   *  PLUSONE);
		corners[4].setY(center.getY() + dimensions.getY()   * MINUSONE);
		corners[4].setZ(center.getZ() + dimensions.getZ()   * MINUSONE);
		//i=1 j=0 k=1
		corners[5].setX(center.getX() + dimensions.getX()   *  PLUSONE);
		corners[5].setY(center.getY() + dimensions.getY()   * MINUSONE);
		corners[5].setZ(center.getZ() + dimensions.getZ()   *  PLUSONE);
		//i=1 j=1 k=0
		corners[6].setX(center.getX() + dimensions.getX()   *  PLUSONE);
		corners[6].setY(center.getY() + dimensions.getY()   *  PLUSONE);
		corners[6].setZ(center.getZ() + dimensions.getZ()   * MINUSONE);
		//i=1 j=1 k=1
		corners[7].setX(center.getX() + dimensions.getX()   *  PLUSONE);
		corners[7].setY(center.getY() + dimensions.getY()   *  PLUSONE);
		corners[7].setZ(center.getZ() + dimensions.getZ()   *  PLUSONE);
		
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

		modelViewMatrix4f.translate(pos.castToSinglePrecision());
		computeFrustrumPlanes();
		updateMatricesForShaderUniforms();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#setupShader(io.xol.engine.graphics.shaders.ShaderProgram)
	 */
	@Override
	public void setupShader(ShaderInterface shaderInterface)
	{
		// Helper function to clean code from messy bits :)
		shaderInterface.setUniformMatrix4f("projectionMatrix", projectionMatrix4f);
		shaderInterface.setUniformMatrix4f("projectionMatrixInv", projectionMatrix4fInverted);

		shaderInterface.setUniformMatrix4f("modelViewMatrix", modelViewMatrix4f);
		shaderInterface.setUniformMatrix4f("modelViewMatrixInv", modelViewMatrix4fInverted);

		shaderInterface.setUniformMatrix3f("normalMatrix", normalMatrix3f);
		shaderInterface.setUniformMatrix3f("normalMatrixInv", normalMatrix3fInverted);
		
		shaderInterface.setUniformMatrix4f("modelViewProjectionMatrix", modelViewProjectionMatrix4f);
		shaderInterface.setUniformMatrix4f("modelViewProjectionMatrixInv", modelViewProjectionMatrix4fInverted);
		
		shaderInterface.setUniformMatrix4f("untranslatedMV", untranslatedMVP4f);
		shaderInterface.setUniformMatrix4f("untranslatedMVInv", untranslatedMVP4fInv);
		
		shaderInterface.setUniform2f("screenViewportSize", this.viewportWidth, this.viewportHeight);

		shaderInterface.setUniform3f("camPos", getCameraPosition());
	}
	
	public Vector3fm transform3DCoordinate(Vector3fm in)
	{
		return transform3DCoordinate(new Vector4fm(in.getX(), in.getY(), in.getZ(), 1f));
	}
	
	/**
	 * Spits out where some point in world coordinates ends up on the screen
	 * @param in
	 * @return
	 */
	public Vector3fm transform3DCoordinate(Vector4fm in)
	{
		//position = new Vector4fm(-(float)e.posX, -(float)e.posY, -(float)e.posZ, 1f);
		//position = new Vector4fm(1f, 1f, 1f, 1);
		Matrix4f mvm = this.modelViewMatrix4f;
		Matrix4f pm = this.projectionMatrix4f;

		//Matrix4f combined = Matrix4f.mul(pm, mvm, null);
		
		in = Matrix4f.transform(mvm, in, null);
		// transformed.scale(1/transformed.w);
		in = Matrix4f.transform(pm, in, null);
		
		//in = Matrix4f.transform(combined, in, null);

		//position.scale(1/position.w);

		Vector3fm posOnScreen = new Vector3fm((float)in.getX(), (float)in.getY(), 0f);
		float scale = 1/in.getZ();
		posOnScreen.scale(scale);

		posOnScreen.setX((posOnScreen.getX() * 0.5f + 0.5f) * viewportWidth);
		posOnScreen.setY(((posOnScreen.getY() * 0.5f + 0.5f)) * viewportHeight);
		posOnScreen.setZ(scale);
		return posOnScreen;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#getViewDirection()
	 */
	@Override
	public Vector3fm getViewDirection()
	{
		float rotH = rotationY;
		float rotV = rotationX;
		float a = (float) ((180-rotH) / 180f * Math.PI);
		float b = (float) ((-rotV) / 180f * Math.PI);
		return new Vector3fm((float) (Math.sin(a) * Math.cos(b)),(float)( Math.sin(b)) , (float)(Math.cos(a) * Math.cos(b)));
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.CameraInterface#getCameraPosition()
	 */
	@Override
	public Vector3dm getCameraPosition()
	{
		return this.pos.clone().negate();
	}

	@Override
	public void setCameraPosition(Vector3<?> pos)
	{
		this.pos = new Vector3dm(pos).negate();
	}
}
