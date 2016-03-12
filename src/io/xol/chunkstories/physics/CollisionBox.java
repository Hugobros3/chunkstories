package io.xol.chunkstories.physics;

import static io.xol.chunkstories.renderer.debug.OverlayRenderer.*;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.World;

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

	public boolean collidesWith(World world)
	{
		if (VoxelTypes.get(world.getDataAt((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos + xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos - xw / 2), (int) (ypos), (int) (zpos + zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos + xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos + xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos - xw / 2), (int) (ypos + h), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		if (VoxelTypes.get(world.getDataAt((int) (xpos - xw / 2), (int) (ypos), (int) (zpos - zw / 2))).isVoxelSolid())
			return true;
		return false;
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

	public String toString()
	{
		return "Collision Box : position = [" + xpos + ", " + ypos + ", " + zpos + "] size = [" + xw + ", " + h + ", " + zw + "]";
	}

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
