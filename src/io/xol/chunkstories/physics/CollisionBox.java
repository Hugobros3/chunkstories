package io.xol.chunkstories.physics;

import static io.xol.chunkstories.renderer.debug.OverlayRenderer.*;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class CollisionBox implements Collidable
{
	public double xpos, ypos, zpos;
	public double xw, h, zw;

	public CollisionBox(CollisionBox box)
	{
		xw = box.xw;
		h = box.h;
		zw = box.zw;
		xpos = box.xpos;
		ypos = box.ypos;
		zpos = box.zpos;
	}

	public CollisionBox(double xwidth, double height, double zwidth)
	{
		xw = xwidth;
		h = height;
		zw = zwidth;
	}

	public CollisionBox translate(double x, double y, double z)
	{
		xpos += x;
		ypos += y;
		zpos += z;
		return this;
	}
	
	public CollisionBox translate(Vector3d vec3)
	{
		xpos += vec3.x;
		ypos += vec3.y;
		zpos += vec3.z;
		return this;
	}

	public boolean collidesWith(World world)
	{
		if (VoxelTypes.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		return false;
	}

	boolean getLineIntersection(double fDst1, double fDst2, Vector3d P1, Vector3d P2, Vector3d hit)
	{
		if ((fDst1 * fDst2) >= 0.0f)
			return false;
		if (fDst1 == fDst2)
			return false;

		Vector3d TP1 = new Vector3d();
		Vector3d TP2 = new Vector3d();
		TP1.set(P1);
		TP2.set(P2);
		
		Vector3d tempHit = new Vector3d();
		Vector3d.sub(TP2, TP1, tempHit);
		tempHit.scale(-fDst1 / (fDst2 - fDst1));
		TP1.set(P1);
		//System.out.println("tmp: "+tempHit + "tp1" + TP1);
		tempHit = Vector3d.add(TP1, tempHit, null);
		//System.out.println("tmp2: "+tempHit);
		//Vector3d temphit = TP1.add( ( TP2.sub(TP1) ).scale(-fDst1 / (fDst2 - fDst1)) );
		hit.set(tempHit);
		return true;
	}

	boolean inBox(Vector3d hit, Vector3d B1, Vector3d B2, int axis)
	{
		if (axis == 1 && hit.z > B1.z && hit.z < B2.z && hit.y > B1.y && hit.y < B2.y)
			return true;
		if (axis == 2 && hit.z > B1.z && hit.z < B2.z && hit.x > B1.x && hit.x < B2.x)
			return true;
		if (axis == 3 && hit.x > B1.x && hit.x < B2.x && hit.y > B1.y && hit.y < B2.y)
			return true;
		return false;
	}

	/**
	 * Box / Line collision check
	 * Returns null if no colision, a Vector3d if collision, containing the collision point.
	 * @return The collision point, or NULL.
	 */
	public Vector3d collidesWith(Vector3d lineStart, Vector3d lineDirection)
	{
		Vector3d B1 = new Vector3d(xpos - xw/2, ypos, zpos - zw/2);
		Vector3d B2 = new Vector3d(xpos + xw/2, ypos + h, zpos + zw/2);
		
		
		Vector3d L1 = new Vector3d();
		L1.set(lineStart);
		Vector3d L2 = new Vector3d();
		L2.set(lineDirection);
		L2.scale(500);
		L2.add(lineStart);
		//System.out.println(xpos + ": " + ypos + ": " + zpos);
		//System.out.println(L1 + " : " + L2);
		
		if (L2.x < B1.x && L1.x < B1.x)
			return null;
		if (L2.x > B2.x && L1.x > B2.x)
			return null;
		if (L2.y < B1.y && L1.y < B1.y)
			return null;
		if (L2.y > B2.y && L1.y > B2.y)
			return null;
		if (L2.z < B1.z && L1.z < B1.z)
			return null;
		if (L2.z > B2.z && L1.z > B2.z)
			return null;
		if (L1.x > B1.x && L1.x < B2.x && L1.y > B1.y && L1.y < B2.y && L1.z > B1.z && L1.z < B2.z)
		{
			return L1;
		}
		Vector3d hit = new Vector3d();
		if ((getLineIntersection(L1.x - B1.x, L2.x - B1.x, L1, L2, hit) && inBox(hit, B1, B2, 1)) || (getLineIntersection(L1.y - B1.y, L2.y - B1.y, L1, L2, hit) && inBox(hit, B1, B2, 2))
				|| (getLineIntersection(L1.z - B1.z, L2.z - B1.z, L1, L2, hit) && inBox(hit, B1, B2, 3)) || (getLineIntersection(L1.x - B2.x, L2.x - B2.x, L1, L2, hit) && inBox(hit, B1, B2, 1))
				|| (getLineIntersection(L1.y - B2.y, L2.y - B2.y, L1, L2, hit) && inBox(hit, B1, B2, 2)) || (getLineIntersection(L1.z - B2.z, L2.z - B2.z, L1, L2, hit) && inBox(hit, B1, B2, 3)))
			return hit;

		//sSystem.out.println(hit);
		return null;
	}

	public double getWidthWarp()
	{
		return xw % 1;
	}

	public double getLengthWarp()
	{
		return zw % 1;
	}

	public double getHeightWarp()
	{
		return h % 1;
	}

	public void debugDraw(float r, float g, float b, float a)
	{
		// glTranslated(xpos-xw/2,ypos,zpos-zw/2);

		//System.out.println("Debug drawing at "+xpos+" y:"+ypos+" z:"+(zpos-zw/2));

		glDisable(GL_TEXTURE_2D);
		glColor4f(r, g, b, a);
		glLineWidth(2);
		glDisable(GL_CULL_FACE);
		//glDepthFunc(GL_LEQUAL);
		// glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		// glBlendFunc(GL_ONE_MINUS_SRC_COLOR,GL_ONE);
		glBegin(GL_LINES);
		glVertex3d(xpos - xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos, zpos + zw / 2);

		glVertex3d(xpos - xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos + h, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos + h, zpos + zw / 2);

		glVertex3d(xpos - xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos - xw / 2, ypos, zpos + zw / 2);
		glVertex3d(xpos - xw / 2, ypos + h, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos - zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos - zw / 2);
		glVertex3d(xpos + xw / 2, ypos, zpos + zw / 2);
		glVertex3d(xpos + xw / 2, ypos + h, zpos + zw / 2);
		glEnd();
	}

	@Override
	public String toString()
	{
		return "Collision Box : position = [" + xpos + ", " + ypos + ", " + zpos + "] size = [" + xw + ", " + h + ", " + zw + "]";
	}

	@Override
	public boolean collidesWith(CollisionBox b)
	{
		if (ypos + h <= b.ypos || ypos >= b.ypos + b.h || xpos + xw / 2.0 <= b.xpos - b.xw / 2.0 || xpos - xw / 2.0 >= b.xpos + b.xw / 2.0 || zpos + zw / 2.0 <= b.zpos - b.zw / 2.0 || zpos - zw / 2.0 >= b.zpos + b.zw / 2.0)
		{
			return false;
		}
		// System.out.println(this.toString()+":"+b.toString());
		return true;
	}

	public boolean isPointInside(double posX, double posY, double posZ)
	{
		if (ypos + h < posY || ypos > posY || xpos + xw / 2.0 < posX || xpos - xw / 2.0 > posX || zpos + zw / 2.0 < posZ || zpos - zw / 2.0 > posZ)
		{
			return false;
		}

		return false;
	}

	public boolean collidesWith(Entity entity)
	{
		CollisionBox[] entityBoxes = entity.getTranslatedCollisionBoxes();
		if (entityBoxes != null)
			for (CollisionBox entityBox : entityBoxes)
				if (entityBox.collidesWith(entityBox))
					return true;
		return false;
	}
}
