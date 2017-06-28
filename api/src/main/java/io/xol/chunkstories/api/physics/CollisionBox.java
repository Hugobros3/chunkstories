package io.xol.chunkstories.api.physics;

import io.xol.chunkstories.api.Content.Voxels;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public final class CollisionBox implements Collidable
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

	public CollisionBox(double xpos, double ypos, double zpos, double xw, double h, double zw)
	{
		this.xpos = xpos;
		this.ypos = ypos;
		this.zpos = zpos;
		this.xw = xw;
		this.h = h;
		this.zw = zw;
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
		Voxels store = world.getGameContext().getContent().voxels();
		if (store.getVoxelById(world.getVoxelData((int) (xpos + xw), (int) (ypos + h), (int) (zpos + zw))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos + xw), (int) (ypos), (int) (zpos + zw))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos), (int) (ypos + h), (int) (zpos + zw))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos), (int) (ypos), (int) (zpos + zw))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos + xw), (int) (ypos + h), (int) (zpos))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos + xw), (int) (ypos), (int) (zpos))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos), (int) (ypos + h), (int) (zpos))).getType().isSolid())
			return true;
		if (store.getVoxelById(world.getVoxelData((int) (xpos), (int) (ypos), (int) (zpos))).getType().isSolid())
			return true;
		return false;
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
	 * Box / Line collision check Returns null if no collision, a Vector3dm if collision, containing the collision point.
	 * 
	 * @return The collision point, or NULL.
	 */
	public Vector3dm lineIntersection(Vector3dm lineStart, Vector3dm lineDirection)
	{
		double minDist = 0.0;
		double maxDist = 256d;
		
		Vector3dm min = new Vector3dm(xpos, ypos, zpos);
		Vector3dm max = new Vector3dm(xpos + xw, ypos + h, zpos + zw);
		
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

	@Override
	public String toString()
	{
		return "Collision Box : position = [" + xpos + ", " + ypos + ", " + zpos + "] size = [" + xw + ", " + h + ", " + zw + "]";
	}

	@Override
	public boolean collidesWith(Collidable c)
	{
		if(c instanceof CollisionBox)
			return collidesWith(c);
		
		throw new UnsupportedOperationException("Unupported Collidable: "+c.getClass().getSimpleName());
	}
	
	public boolean collidesWith(CollisionBox b)
	{
		if (ypos + h <= b.ypos || ypos >= b.ypos + b.h || xpos + xw<= b.xpos || xpos >= b.xpos + b.xw || zpos + zw <= b.zpos || zpos >= b.zpos + b.zw)
		{
			return false;
		}
		return true;
	}

	public boolean isPointInside(double posX, double posY, double posZ)
	{
		if (ypos + h < posY || ypos > posY || xpos + xw < posX || xpos > posX || zpos + zw < posZ || zpos > posZ)
		{
			return false;
		}

		return false;
	}

	/*public boolean collidesWith(Entity entity)
	{
		CollisionBox[] entityBoxes = entity.getTranslatedCollisionBoxes();
		if (entityBoxes != null)
			for (CollisionBox entityBox : entityBoxes)
				if (entityBox.collidesWith(entityBox))
					return true;
		return false;
	}*/
}
