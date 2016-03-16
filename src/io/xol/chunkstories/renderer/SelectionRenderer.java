package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.renderer.debug.OverlayRenderer.*;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.world.World;

//(c) 2-0.05f15 XolioWare Interactive

public class SelectionRenderer
{
	World world;
	WorldRenderer worldRenderer;

	public SelectionRenderer(World w, WorldRenderer r)
	{
		world = w;
		worldRenderer = r;
	}

	public void drawSelectionBox(int x, int y, int z)
	{
		glDisable(GL_TEXTURE_2D);
		glColor4f(1, 1, 1, 1f);
		glLineWidth(2);
		glDisable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		// glBlendFunc(GL_ONE_MINUS_SRC_COLOR,GL_ONE);
		glBegin(GL_LINES);
		BlockRenderInfo bri = new BlockRenderInfo(world, x, y, z);
		if(bri.voxelType == null)
		{
			System.out.println(bri.data);
			return;
		}
		for(CollisionBox box : bri.
				voxelType
				.getTranslatedCollisionBoxes(world, x, y, z))
			cubeVertices((float)box.xpos, (float)box.ypos, (float)box.zpos, (float)box.xw, (float)box.h, (float)box.zw);
		glEnd();
		glColor4f(1, 1, 1, 1);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
	}
	
	private void cubeVertices(float x, float y, float z, float xw, float h, float zw)
	{
		glVertex3f(-xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, 0 + y, zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, 0 + y, zw/2f + z);

		glVertex3f(-xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, +h + y, zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, +h + y, zw/2f + z);

		glVertex3f(-xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(-xw/2f + x, 0 + y, zw/2f + z);
		glVertex3f(-xw/2f + x, +h + y, zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, -zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, -zw/2f + z);
		glVertex3f(xw/2f + x, 0 + y, zw/2f + z);
		glVertex3f(xw/2f + x, +h + y, zw/2f + z);
	}
}
