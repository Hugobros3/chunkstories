package io.xol.chunkstories.renderer;

import static org.lwjgl.opengl.GL11.*;
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

	void translate(float x, float y, float z)
	{
		// Will setup rendering/shader status to correct position (
		// world-wrapping corrected )
		glTranslatef(x, y, z);
	}

	public void drawSelectionBox(int x, int y, int z)
	{
		translate(x, y, z);
		glDisable(GL_TEXTURE_2D);
		glColor4f(1, 1, 1, 1f);
		glLineWidth(1);
		glDisable(GL_CULL_FACE);
		glEnable(GL_ALPHA_TEST);
		glDisable(GL_BLEND);
		// glBlendFunc(GL_ONE_MINUS_SRC_COLOR,GL_ONE);
		glBegin(GL_LINES);
		glVertex3f(-0.005f, 0.005f, -0.005f);
		glVertex3f(1.005f, 0.005f, -0.005f);
		glVertex3f(-0.005f, 0.005f, 1.005f);
		glVertex3f(1.005f, 0.005f, 1.005f);
		glVertex3f(1.005f, 0.005f, 1.005f);
		glVertex3f(1.005f, 0.005f, -0.005f);
		glVertex3f(-0.005f, 0.005f, -0.005f);
		glVertex3f(-0.005f, 0.005f, 1.005f);

		glVertex3f(-0.005f, -1.005f, -0.005f);
		glVertex3f(1.005f, -1.005f, -0.005f);
		glVertex3f(-0.005f, -1.005f, 1.005f);
		glVertex3f(1.005f, -1.005f, 1.005f);
		glVertex3f(1.005f, -1.005f, 1.005f);
		glVertex3f(1.005f, -1.005f, -0.005f);
		glVertex3f(-0.005f, -1.005f, -0.005f);
		glVertex3f(-0.005f, -1.005f, 1.005f);

		glVertex3f(-0.005f, 0.005f, -0.005f);
		glVertex3f(-0.005f, -1.005f, -0.005f);
		glVertex3f(-0.005f, 0.005f, 1.005f);
		glVertex3f(-0.005f, -1.005f, 1.005f);
		glVertex3f(1.005f, 0.005f, -0.005f);
		glVertex3f(1.005f, -1.005f, -0.005f);
		glVertex3f(1.005f, 0.005f, 1.005f);
		glVertex3f(1.005f, -1.005f, 1.005f);
		glEnd();
		glColor4f(1, 1, 1, 1);
		glEnable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_BLEND);

		glTranslatef(-x, -y, -z);
	}

}
