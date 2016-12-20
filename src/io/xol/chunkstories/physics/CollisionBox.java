package io.xol.chunkstories.physics;

import static io.xol.chunkstories.renderer.debug.OverlayRenderer.*;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

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

	public CollisionBox translate(Vector3dm vec3)
	{
		xpos += vec3.getX();
		ypos += vec3.getY();
		zpos += vec3.getZ();
		return this;
	}

	public boolean collidesWith(World world)
	{
		if (Voxels.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos + xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (Voxels.get(world.getVoxelData((int) (xpos - xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		return false;
	}

	boolean getLineIntersection(double fDst1, double fDst2, Vector3dm P1, Vector3dm P2, Vector3dm hit)
	{
		if ((fDst1 * fDst2) >= 0.0f)
			return false;
		if (fDst1 == fDst2)
			return false;

		Vector3dm TP1 = new Vector3dm();
		Vector3dm TP2 = new Vector3dm();
		TP1.set(P1);
		TP2.set(P2);

		Vector3dm tempHit = new Vector3dm(TP2);
		
		tempHit.sub(TP1);
		//Vector3dm.sub(TP2, TP1, tempHit);
		
		tempHit.scale(-fDst1 / (fDst2 - fDst1));
		TP1.set(P1);
		
		tempHit.add(TP1);
		//tempHit = Vector3dm.add(TP1, tempHit, null);
		
		//System.out.println("tmp2: "+tempHit);
		//Vector3dm temphit = TP1.add( ( TP2.sub(TP1) ).scale(-fDst1 / (fDst2 - fDst1)) );
		hit.set(tempHit);
		return true;
	}

	boolean inBox(Vector3dm hit, Vector3dm B1, Vector3dm B2, int axis)
	{
		if (axis == 1 && hit.getZ() > B1.getZ() && hit.getZ() < B2.getZ() && hit.getY() > B1.getY() && hit.getY() < B2.getY())
			return true;
		if (axis == 2 && hit.getZ() > B1.getZ() && hit.getZ() < B2.getZ() && hit.getX() > B1.getX() && hit.getX() < B2.getX())
			return true;
		if (axis == 3 && hit.getX() > B1.getX() && hit.getX() < B2.getX() && hit.getY() > B1.getY() && hit.getY() < B2.getY())
			return true;
		return false;
	}

	/**
	 * Box / Line collision check Returns null if no colision, a Vector3dm if collision, containing the collision point.
	 * 
	 * @return The collision point, or NULL.
	 */
	public Vector3dm collidesWith(Vector3dm lineStart, Vector3dm lineDirection)
	{
		double minDist = 0.0;
		double maxDist = 256d;
		
		Vector3dm min = new Vector3dm(xpos - xw / 2, ypos, zpos - zw / 2);
		Vector3dm max = new Vector3dm(xpos + xw / 2, ypos + h, zpos + zw / 2);
		
		lineDirection.normalize();
		
		Vector3dm invDir = new Vector3dm(1f / lineDirection.getX(), 1f / lineDirection.getY(), 1f / lineDirection.getZ());

		boolean signDirX = invDir.getX() < 0;
		boolean signDirY = invDir.getY() < 0;
		boolean signDirZ = invDir.getZ() < 0;

		Vector3dm bbox = signDirX ? max : min;
		double tmin = (bbox.getX() - lineStart.getX()) * invDir.getX();
		bbox = signDirX ? min : max;
		double tmax = (bbox.getX() - lineStart.getX()) * invDir.getX();
		bbox = signDirY ? max : min;
		double tymin = (bbox.getY() - lineStart.getY()) * invDir.getY();
		bbox = signDirY ? min : max;
		double tymax = (bbox.getY() - lineStart.getY()) * invDir.getY();

		if ((tmin > tymax) || (tymin > tmax)) {
			return null;
		}
		if (tymin > tmin) {
			tmin = tymin;
		}
		if (tymax < tmax) {
			tmax = tymax;
		}

		bbox = signDirZ ? max : min;
		double tzmin = (bbox.getZ() - lineStart.getZ()) * invDir.getZ();
		bbox = signDirZ ? min : max;
		double tzmax = (bbox.getZ() - lineStart.getZ()) * invDir.getZ();

		if ((tmin > tzmax) || (tzmin > tmax)) {
			return null;
		}
		if (tzmin > tmin) {
			tmin = tzmin;
		}
		if (tzmax < tmax) {
			tmax = tzmax;
		}
		if ((tmin < maxDist) && (tmax > minDist)) {
			
			Vector3dm intersect = new Vector3dm(lineStart);
			
			intersect.add(lineDirection.clone().normalize().scale(tmin));
			return intersect;
			//return Vector3dm.add(lineStart, lineDirection.clone().normalize().scale(tmin), null);
			
			//return ray.getPointAtDistance(tmin);
		}
		return null;
		
	}
	/*public Vector3dm collidesWith(Vector3dm lineStart, Vector3dm lineDirection)
	{
		Vector3dm B1 = new Vector3dm(xpos - xw / 2, ypos, zpos - zw / 2);
		Vector3dm B2 = new Vector3dm(xpos + xw / 2, ypos + h, zpos + zw / 2);

		Vector3dm L1 = new Vector3dm();
		L1.set(lineStart);
		Vector3dm L2 = new Vector3dm();
		L2.set(lineDirection);
		L2.scale(500);
		L2.add(lineStart);

		if (L2.getX() < B1.getX() && L1.getX() < B1.getX())
			return null;
		if (L2.getX() > B2.getX() && L1.getX() > B2.getX())
			return null;
		if (L2.getY() < B1.getY() && L1.getY() < B1.getY())
			return null;
		if (L2.getY() > B2.getY() && L1.getY() > B2.getY())
			return null;
		if (L2.getZ() < B1.getZ() && L1.getZ() < B1.getZ())
			return null;
		if (L2.getZ() > B2.getZ() && L1.getZ() > B2.getZ())
			return null;
		if (L1.getX() > B1.getX() && L1.getX() < B2.getX() && L1.getY() > B1.getY() && L1.getY() < B2.getY() && L1.getZ() > B1.getZ() && L1.getZ() < B2.getZ())
		{
			System.out.println("L1" + L1);
			return L1;
		}
		//System.out.println("c kompliker");
		Vector3dm possibleHit = null;
		Vector3dm hit = new Vector3dm();
		if ((getLineIntersection(L1.getX() - B1.getX(), L2.getX() - B1.getX(), L1, L2, hit) && inBox(hit, B1, B2, 1)))
		{
			System.out.println("x1");
			
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		if ((getLineIntersection(L1.getY() - B1.getY(), L2.getY() - B1.getY(), L1, L2, hit) && inBox(hit, B1, B2, 2)))
		{
			System.out.println("y1");
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		if ((getLineIntersection(L1.getZ() - B1.getZ(), L2.getZ() - B1.getZ(), L1, L2, hit) && inBox(hit, B1, B2, 3)))
		{
			System.out.println("z1");
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		if ((getLineIntersection(L1.getX() - B2.getX(), L2.getX() - B2.getX(), L1, L2, hit) && inBox(hit, B1, B2, 1)))
		{
			System.out.println("y1");
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		if ((getLineIntersection(L1.getY() - B2.getY(), L2.getY() - B2.getY(), L1, L2, hit) && inBox(hit, B1, B2, 2)))
		{
			System.out.println("y2");
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		if ((getLineIntersection(L1.getZ() - B2.getZ(), L2.getZ() - B2.getZ(), L1, L2, hit) && inBox(hit, B1, B2, 3)))
		{
			System.out.println("z2");
			if(true)
				return hit;
			if(possibleHit == null || possibleHit.distanceTo(lineStart) > hit.distanceTo(lineStart))
				possibleHit = hit;
		}
		//		)
		//	return hit;

		//sSystem.out.println(hit);
		return possibleHit;
	}*/
	
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
		glColor4f(r, g, b, a);
		glDisable(GL_CULL_FACE);
		
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
