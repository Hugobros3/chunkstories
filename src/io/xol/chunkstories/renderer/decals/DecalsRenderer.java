package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.lalgb.Matrix3d;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsRenderer
{
	VerticesObject verticesObject = new VerticesObject();
	WorldRenderer worldRenderer;
	
	public DecalsRenderer(WorldRenderer worldRenderer)
	{
		this.worldRenderer = worldRenderer;
	}
	
	public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, Texture2D texture )
	{
		ByteBuffer bbuf = BufferUtils.createByteBuffer(512);
		Matrix3f alignedToDecalSpace = new Matrix3f();
		//up = (forward cross (0, 1, 0)) cross forward 
	}
	
	public void renderDecals(RenderingContext renderingContext)
	{
		Vector3d orthogonalRight = new Vector3d(1.0, 0.0, 0.0);
		Vector3d orthogonalTop = new Vector3d(0.0, 1.0, 0.0);
		Vector3d orthogonalForward = new Vector3d(0.0, 0.0, 1.0);
		
		Vector3d originalPosition = new Vector3d(2512, 31, 2027);
		Vector3d rayDirection = new Vector3d(0.33, 0.45, -0.6);
		rayDirection.normalize();
		
		//Stack overflow answer to how to build matrix to rotate one unit vector to another
		/*Vector3d v = Vector3d.cross(orthogonalForward, rayDirection);
		double s = v.length();
		double c = Vector3d.dot(orthogonalForward, rayDirection);
		
		System.out.println(c+" "+s+" chksm: "+(c*c + s*s) + "v:"+v);
		
		Matrix3d skewSymetricCrosProductMatrixOfV = new Matrix3d();
		skewSymetricCrosProductMatrixOfV.setZero();
		skewSymetricCrosProductMatrixOfV.m00 = 0;
		skewSymetricCrosProductMatrixOfV.m10 = -v.z;
		skewSymetricCrosProductMatrixOfV.m20 = v.y;
		
		skewSymetricCrosProductMatrixOfV.m01 = v.z;
		skewSymetricCrosProductMatrixOfV.m11 = 0;
		skewSymetricCrosProductMatrixOfV.m21 = -v.x;
		
		skewSymetricCrosProductMatrixOfV.m02 = -v.y;
		skewSymetricCrosProductMatrixOfV.m12 = v.x;
		skewSymetricCrosProductMatrixOfV.m22 = 0;
		
		Matrix3d skewSymetricCrosProductMatrixOfV_squared = new Matrix3d(skewSymetricCrosProductMatrixOfV);
				
		skewSymetricCrosProductMatrixOfV_squared = Matrix3d.mul(skewSymetricCrosProductMatrixOfV_squared, skewSymetricCrosProductMatrixOfV_squared, null);

		double factor = (1.0 - c) / (s * s);
		skewSymetricCrosProductMatrixOfV_squared.scale(factor);
		
		Matrix3d rotationMatrix = new Matrix3d();
		Matrix3d.add(rotationMatrix, skewSymetricCrosProductMatrixOfV, rotationMatrix);
		Matrix3d.add(rotationMatrix, skewSymetricCrosProductMatrixOfV_squared, rotationMatrix);
		
		*/
		
		Vector3d up = new Vector3d(0.0, 1.0, 0.0);
		Vector3d.cross(rayDirection, new Vector3d(0.0, 1.0, 0.0), up);
		Vector3d.cross(up, rayDirection, up);
		up.normalize();
		
		Vector3d right = new Vector3d();
		Vector3d.cross(rayDirection, new Vector3d(1.0, 0.0, 0.0), right);
		Vector3d.cross(right, rayDirection, right);
		right.normalize();
		
		Matrix3d rotationMatrix = new Matrix3d(right, up, rayDirection);
		
		System.out.println(rotationMatrix);
		
		Vector3d result = Matrix3d.transform(orthogonalForward, rotationMatrix, null);
		
		System.out.println("Target : " + rayDirection);
		System.out.println("Result : " + result);
		
		
		SelectionRenderer.cubeVertices((float)originalPosition.x, (float)originalPosition.y, (float)originalPosition.z, 0.05f, 0.05f, 0.05f);

		Vector3d finalPosition = originalPosition.clone().add(up);
		OverlayRenderer.glLineWidth(1f);
		OverlayRenderer.glBegin(OverlayRenderer.GL_LINES);
		OverlayRenderer.glColor4f(1.0f, 1, 1, 1.0f);
		OverlayRenderer.glVertex3d(originalPosition.x, originalPosition.y, originalPosition.z);
		OverlayRenderer.glVertex3d(finalPosition.x, finalPosition.y, finalPosition.z);
		OverlayRenderer.glEnd();
		
		finalPosition = originalPosition.clone().add(right);
		OverlayRenderer.glLineWidth(1.5f);
		OverlayRenderer.glBegin(OverlayRenderer.GL_LINES);
		OverlayRenderer.glColor4f(1.0f, 1, 1, 1.0f);
		OverlayRenderer.glVertex3d(originalPosition.x, originalPosition.y, originalPosition.z);
		OverlayRenderer.glVertex3d(finalPosition.x, finalPosition.y, finalPosition.z);
		OverlayRenderer.glEnd();
		
		finalPosition = originalPosition.clone().add(rayDirection);
		OverlayRenderer.glLineWidth(2f);
		OverlayRenderer.glBegin(OverlayRenderer.GL_LINES);
		OverlayRenderer.glColor4f(1.0f, 1, 1, 1.0f);
		OverlayRenderer.glVertex3d(originalPosition.x, originalPosition.y, originalPosition.z);
		OverlayRenderer.glVertex3d(finalPosition.x, finalPosition.y, finalPosition.z);
		OverlayRenderer.glEnd();
		
		
	}
}
