package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.renderer.debug.OverlayRenderer.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.physics.CollisionBox;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SelectionRenderer
{
	World world;
	WorldRenderer worldRenderer;

	public SelectionRenderer(World w, WorldRenderer r)
	{
		world = w;
		worldRenderer = r;
	}

	public void drawSelectionBox(Location location)
	{
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		glColor4f(1, 1, 1, 1.0f);
		//GL11.glBlendFunc(GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ZERO);
		//GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		//GL11.glBlendEquation(GL11.);
		glBegin(GL_LINES);
		VoxelContext bri = new VoxelContext(world, x, y, z);
		if (bri.voxelType == null)
		{
			System.out.println(bri.data);
			return;
		}
		for (CollisionBox box : bri.voxelType.getTranslatedCollisionBoxes(world, x, y, z))
			cubeVertices((float) box.xpos, (float) box.ypos, (float) box.zpos, (float) box.xw, (float) box.h, (float) box.zw);
		glEnd();
		glColor4f(1, 1, 1, 1);
	}

	public static void cubeVertices(float x, float y, float z, float xw, float h, float zw)
	{
		glVertex3f(-xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, 0 + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, 0 + y, zw / 2f + z);

		glVertex3f(-xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, +h + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, +h + y, zw / 2f + z);

		glVertex3f(-xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(-xw / 2f + x, 0 + y, zw / 2f + z);
		glVertex3f(-xw / 2f + x, +h + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, -zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, -zw / 2f + z);
		glVertex3f(xw / 2f + x, 0 + y, zw / 2f + z);
		glVertex3f(xw / 2f + x, +h + y, zw / 2f + z);
	}
}
