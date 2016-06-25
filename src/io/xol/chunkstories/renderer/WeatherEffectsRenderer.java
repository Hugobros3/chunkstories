package io.xol.chunkstories.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import io.xol.engine.math.lalgb.Vector2f;

import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WeatherEffectsRenderer
{
	Random random = new Random();
	WorldImplementation world;
	WorldRenderer worldRenderer;
	
	public WeatherEffectsRenderer(WorldImplementation world, WorldRenderer worldRenderer)
	{
		this.world = world;
		this.worldRenderer = worldRenderer;
	}
	
	//Every second regenerate the buffer with fresh vertices
	float[] raindrops = new float[110000 * 6 * 4];
	FloatBuffer raindropsData = BufferUtils.createFloatBuffer(110000 * 6 * 4);
	// Array setup : 
	int bufferOffset = 0;
	int viewX, viewY, viewZ;
	int lastX, lastY, lastZ;
	
	private void generateRainForOneSecond()
	{
		bufferOffset %= 110000;
		bufferOffset += 10000;
		Vector2f view2drop = new Vector2f();
		for(int i = 0; i < 100000; i++)
		{
			// We want to always leave alone the topmost part of the array until it has gone out of view
			int location = (bufferOffset + i) % 110000;
			//Randomize location
			float rdX = viewX + (random.nextFloat() * 2.0f - 1.0f) * 25;
			float rdY = viewY + (random.nextFloat() * 2.0f - 0.5f) * 20;
			float rdZ = viewZ + (random.nextFloat() * 2.0f - 1.0f) * 25;
			//Max height it can fall to before reverting to used
			float rdMh = world.getRegionSummaries().getHeightAt((int)rdX, (int)rdZ);
			//Raindrop size, change orientation to face viewer
			view2drop.x = rdX - viewX;
			view2drop.y = rdZ - viewZ;
			view2drop.normalise();
			float mx = 0.01f * -view2drop.y;
			float mz = 0.01f * view2drop.x;
			float rainDropletSize = 0.2f;
			//Build triangle strip
			//00
			
			raindrops[location * 6 * 4 + 0 * 4 + 0] = rdX-mx;
			raindrops[location * 6 * 4 + 0 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 0 * 4 + 2] = rdZ-mz;
			raindrops[location * 6 * 4 + 0 * 4 + 3] = rdMh;
			//01
			raindrops[location * 6 * 4 + 1 * 4 + 0] = rdX-mx;
			raindrops[location * 6 * 4 + 1 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 1 * 4 + 2] = rdZ-mz;
			raindrops[location * 6 * 4 + 1 * 4 + 3] = rdMh + rainDropletSize;
			//10
			raindrops[location * 6 * 4 + 2 * 4 + 0] = rdX+mx;
			raindrops[location * 6 * 4 + 2 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 2 * 4 + 2] = rdZ+mz;
			raindrops[location * 6 * 4 + 2 * 4 + 3] = rdMh;
			//11
			raindrops[location * 6 * 4 + 3 * 4 + 0] = rdX-mx;
			raindrops[location * 6 * 4 + 3 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 3 * 4 + 2] = rdZ-mz;
			raindrops[location * 6 * 4 + 3 * 4 + 3] = rdMh + rainDropletSize;
			//01
			raindrops[location * 6 * 4 + 4 * 4 + 0] = rdX+mx;
			raindrops[location * 6 * 4 + 4 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 4 * 4 + 2] = rdZ+mz;
			raindrops[location * 6 * 4 + 4 * 4 + 3] = rdMh;
			//00
			raindrops[location * 6 * 4 + 5 * 4 + 0] = rdX+mx;
			raindrops[location * 6 * 4 + 5 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 5 * 4 + 2] = rdZ+mz;
			raindrops[location * 6 * 4 + 5 * 4 + 3] = rdMh + rainDropletSize;
			
		}
		raindropsData.clear();
		raindropsData.position(0);
		/*for(float[][] r : raindrops) // For each raindrop
			for(float v[] : r) // For each vertice
				for(float c : v) // For each component
				raindropsData.put(c);*/
		
		raindropsData.put(raindrops, 0, raindrops.length);
		raindropsData.flip();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, raindropsData, GL_STATIC_DRAW);
		lastX = viewX;
		lastY = viewY;
		lastZ = viewZ;
	}
	
	long lastRender = 0L;
	
	// Rain falls at ~10m/s, so we prepare in advance 10 meters of rain to fall until we add some more on top
	public void renderEffects(RenderingContext renderingContext)
	{
		viewX = (int) -renderingContext.getCamera().pos.x;
		viewY = (int) -renderingContext.getCamera().pos.y;
		viewZ = (int) -renderingContext.getCamera().pos.z;
		if(world.isRaining())
			renderRain(renderingContext);
	}

	int vboId = -1;
	
	private void renderRain(RenderingContext renderingContext)
	{
		if(vboId == -1)
			vboId = glGenBuffers();
		ShaderProgram weatherShader = ShadersLibrary.getShaderProgram("weather");
		GameWindowOpenGL.getInstance().getRenderingContext().setCurrentShader(weatherShader);
		//weatherShader.use(true);
		if((System.currentTimeMillis() - lastRender) >= 1000 || Math.abs(viewX - lastX) > 10  || Math.abs(viewZ - lastZ) > 10)
		{
			generateRainForOneSecond();
			lastRender = System.currentTimeMillis();
		}
		glDisable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_ALPHA_TEST);
		glDisable(GL_BLEND);
		glDepthFunc(GL_LEQUAL);
		renderingContext.getCamera().setupShader(weatherShader);
		int vertexIn = weatherShader.getVertexAttributeLocation("vertexIn");
		renderingContext.enableVertexAttribute(vertexIn);
		//glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0);
		weatherShader.setUniformFloat("time", (System.currentTimeMillis() - lastRender) / 1000f);
		weatherShader.setUniformSampler(0, "lightmap", TexturesHandler.getTexture("environement/light.png"));
		//weatherShader.setUniformFloat("sunIntensity", worldRenderer.sky.getLightIntensity());
		//raindropsData.flip();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glVertexAttribPointer(vertexIn, 4, GL_FLOAT, false, 0, 0L);
		glDrawArrays(GL_TRIANGLES, 0, 110000);
		renderingContext.disableVertexAttribute(vertexIn);
		glDisable(GL_BLEND);
	}
	
	public void destroy()
	{
		if(vboId != -1)
			glDeleteBuffers(vboId);
	}
}
