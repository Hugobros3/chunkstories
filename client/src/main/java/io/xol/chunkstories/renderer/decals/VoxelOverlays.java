package io.xol.chunkstories.renderer.decals;

import static io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer.*;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.voxel.VoxelContextOlder;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelOverlays
{
	World world;

	public VoxelOverlays(World w)
	{
		world = w;
	}
	
	BreakingBlockDecal dekal = null;
	
	public void drawnCrackedBlocks(RenderingInterface renderingInterface) {
		MiningProgress progress = ItemMiningTool.myProgress;
		if(progress == null || (dekal != null && !dekal.miningProgress.equals(progress))) {
			if(dekal != null) {
				dekal.destroy();
				dekal = null;
			}
		}
		
		if(progress != null) {
			if(dekal == null) {
				dekal = new BreakingBlockDecal(progress);
			}
			dekal.render(renderingInterface);
		}
	}

	public void drawSelectionBox(Location location)
	{
		int x = (int)(double) location.x();
		int y = (int)(double) location.y();
		int z = (int)(double) location.z();
		glColor4f(1, 1, 1, 1.0f);
		//GL11.glBlendFunc(GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ZERO);
		//GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		//GL11.glBlendEquation(GL11.);
		glBegin(GL_LINES);
		VoxelContext bri = new VoxelContextOlder(world, x, y, z);
		if (bri.getVoxel() == null)
		{
			System.out.println(bri.getData());
			return;
		}
		for (CollisionBox box : bri.getVoxel().getTranslatedCollisionBoxes(world, x, y, z))
			cubeVertices((float) box.xpos, (float) box.ypos, (float) box.zpos, (float) box.xw, (float) box.h, (float) box.zw);
		glEnd();
		glColor4f(1, 1, 1, 1);
	}

	public static void cubeVertices(float x, float y, float z, float xw, float h, float zw)
	{
		glVertex3f( + x, 0 + y, + z);
		glVertex3f(xw + x, 0 + y, + z);
		glVertex3f( + x, 0 + y, zw + z);
		glVertex3f(xw + x, 0 + y, zw + z);
		glVertex3f(xw + x, 0 + y, zw + z);
		glVertex3f(xw + x, 0 + y, + z);
		glVertex3f( + x, 0 + y, + z);
		glVertex3f( + x, 0 + y, zw + z);

		glVertex3f( + x, +h + y, + z);
		glVertex3f(xw + x, +h + y, + z);
		glVertex3f( + x, +h + y, zw + z);
		glVertex3f(xw + x, +h + y, zw + z);
		glVertex3f(xw + x, +h + y, zw + z);
		glVertex3f(xw + x, +h + y, + z);
		glVertex3f( + x, +h + y, + z);
		glVertex3f( + x, +h + y, zw + z);

		glVertex3f( + x, 0 + y, + z);
		glVertex3f( + x, +h + y, + z);
		glVertex3f( + x, 0 + y, zw + z);
		glVertex3f( + x, +h + y, zw + z);
		glVertex3f(xw + x, 0 + y, + z);
		glVertex3f(xw + x, +h + y, + z);
		glVertex3f(xw + x, 0 + y, zw + z);
		glVertex3f(xw + x, +h + y, zw + z);
	}
}
