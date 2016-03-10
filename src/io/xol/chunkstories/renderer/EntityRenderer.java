package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.renderer.OverlayRenderer.*;
import io.xol.chunkstories.world.World;

//(c) 2-0.05f15 XolioWare Interactive

public class EntityRenderer
{
	World world;
	WorldRenderer worldRenderer;

	public EntityRenderer(World w, WorldRenderer r)
	{
		world = w;
		worldRenderer = r;
	}

	public void drawSelectionBox(int x, int y, int z)
	{
		glDisable(GL_TEXTURE_2D);
		glColor4f(1, 1, 1, 1f);
		glLineWidth(1);
		glDisable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		// glBlendFunc(GL_ONE_MINUS_SRC_COLOR,GL_ONE);
		glBegin(GL_LINES);
		glVertex3f(-0.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, 0.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, 0.005f + y, 1.005f + z);

		glVertex3f(-0.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, +1.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, +1.005f + y, 1.005f + z);

		glVertex3f(-0.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(-0.005f + x, 0.005f + y, 1.005f + z);
		glVertex3f(-0.005f + x, +1.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, -0.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, -0.005f + z);
		glVertex3f(1.005f + x, 0.005f + y, 1.005f + z);
		glVertex3f(1.005f + x, +1.005f + y, 1.005f + z);
		glEnd();
		glColor4f(1, 1, 1, 1);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
	}
}
