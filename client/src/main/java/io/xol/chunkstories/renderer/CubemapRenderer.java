package io.xol.chunkstories.renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.renderer.WorldRendererImplementation.RenderBuffers;

import static org.lwjgl.opengl.GL11.*;

public class CubemapRenderer {
	private final WorldRendererImplementation worldRenderer;

	public CubemapRenderer(WorldRendererImplementation worldRenderer) {
		super();
		this.worldRenderer = worldRenderer;
	}
	
	public void renderWorldCubemap(RenderingInterface renderingContext, Cubemap cubemap, int resolution, boolean onlyTerrain)
	{
		//lastEnvmapRender = System.currentTimeMillis();
		Client.profiler.startSection("cubemap");

		Camera camera = (Camera) renderingContext.getCamera();
		RenderBuffers buffers = worldRenderer.renderBuffers;

		// Save state
		boolean oldBloom = RenderingConfig.doBloom;
		float oldViewDistance = RenderingConfig.viewDistance;
		RenderingConfig.doBloom = false;
		int oldW = renderingContext.getWindow().getWidth();
		int oldH = renderingContext.getWindow().getHeight();
		float camX = camera.rotationX;
		float camY = camera.rotationY;
		float camZ = camera.rotationZ;
		float fov = camera.fov;
		camera.fov = 45;
		
		int scrW, scrH;
		// Setup cubemap resolution

		if (!onlyTerrain) {
			
			worldRenderer.setupRenderSize(resolution, resolution);
		}
		
		scrW = resolution;
		scrH = resolution;
		

		String[] names = { "front", "back", "top", "bottom", "right", "left" };

		String time = null;
		if (cubemap == null)
		{
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			time = sdf.format(cal.getTime());
		}

		for (int z = 0; z < 6; z++)
		{
			// Camera location
			switch (z)
			{
			case 0:
				camera.rotationX = 0.0f;
				camera.rotationY = 0f;
				break;
			case 1:
				camera.rotationX = 0;
				camera.rotationY = 180;
				break;
			case 2:
				camera.rotationX = -90;
				camera.rotationY = 0;
				break;
			case 3:
				camera.rotationX = 90;
				camera.rotationY = 0;
				break;
			case 4:
				camera.rotationX = 0;
				camera.rotationY = 90;
				break;
			case 5:
				camera.rotationX = 0;
				camera.rotationY = 270;
				break;
			}

			if (onlyTerrain)
				renderingContext.getRenderTargetManager().setConfiguration(buffers.environmentMapFastFbo);
			//environmentMapFastFbo.bind();
			else
				renderingContext.getRenderTargetManager().setConfiguration(buffers.fboShadedBuffer);
			//this.fboShadedBuffer.bind();

			renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();
			//glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			camera.setupUsingScreenSize(scrW, scrH);
			
			// Scene rendering
			if (onlyTerrain)
			{
				//Draw sky
				worldRenderer.getSky().render(renderingContext);

				worldRenderer.getFarTerrainRenderer().renderTerrain(renderingContext, null);
			}
			else
				worldRenderer.renderWorldInternal(renderingContext);

			if (cubemap != null)
			{
				int t[] = new int[] { 4, 5, 3, 2, 0, 1 };
				int f = t[z];

				renderingContext.useShader("blit");

				renderingContext.getRenderTargetManager().setConfiguration(buffers.environmentMapFBO);
				//this.environmentMapFBO.bind();
				buffers.environmentMapFBO.setColorAttachement(0, cubemap.getFace(f));

				if (onlyTerrain)
					renderingContext.bindTexture2D("diffuseTexture", buffers.environmentMapBufferHDR);
				else
					renderingContext.bindTexture2D("diffuseTexture", buffers.shadedBuffer);

				renderingContext.currentShader().setUniform2f("screenSize", resolution, resolution);

				renderingContext.drawFSQuad();
			}
			else
			{
				// GL access
				
				glBindTexture(GL_TEXTURE_2D, buffers.shadedBuffer.getId());
				//glBindTexture(GL_TEXTURE_2D, environmentMapBufferHDR.getId());

				// File access
				File image = new File(GameDirectory.getGameFolderPath() + "/skyboxscreens/" + time + "/" + names[z] + ".png");
				image.mkdirs();

				ByteBuffer bbuf = ByteBuffer.allocateDirect(scrW * scrH * 4 * 4).order(ByteOrder.nativeOrder());
				glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_FLOAT, bbuf);
				System.out.println("Took side "+z);

				BufferedImage pixels = new BufferedImage(scrW, scrH, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < scrW; x++)
					for (int y = 0; y < scrH; y++)
					{
						/*bbuf.getFloat();
						if(x % 500 == 0)
							System.out.println(bbuf.getFloat());*/
						
						int i = 4 * (x + scrW * y);
						int r = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int g = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 4)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						int b = (int) Math2.clamp(Math.pow((bbuf.getFloat(i * 4 + 8)) / 1d, 1d / 2.2d) * 255d, 0.0, 255.0);
						pixels.setRGB(x, scrH - 1 - y, (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | b & 0xFF);

						//System.out.println(bbuf.getFloat(i * 4));
					}
				try
				{
					ImageIO.write(pixels, "PNG", image);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

		}

		// Revert previous data
		RenderingConfig.viewDistance = oldViewDistance;
		RenderingConfig.doBloom = oldBloom;
		camera.rotationX = camX;
		camera.rotationY = camY;
		camera.rotationZ = camZ;
		camera.fov = fov;

		if (!onlyTerrain)
			worldRenderer.setupRenderSize();
		else
		{
			scrW = oldW;
			scrH = oldH;
		}
	}

}
