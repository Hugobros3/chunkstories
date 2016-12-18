package io.xol.chunkstories.renderer;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.Math2;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefaultWeatherEffectsRenderer implements WorldEffectsRenderer
{
	private Random random = new Random();
	private WorldClientCommon world;
	private WorldRenderer worldRenderer;

	public DefaultWeatherEffectsRenderer(WorldClientCommon world, WorldRenderer worldRenderer)
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

	VerticesObject rainVerticesBuffer = new VerticesObject();

	private void generateRainForOneSecond(RenderingInterface renderingContex, float rainPresence)
	{
		float rainIntensity = Math.min(Math.max(0.0f, rainPresence - 0.5f) / 0.3f, 1.0f);

		bufferOffset %= 110000;
		bufferOffset += 10000;
		Vector2f view2drop = new Vector2f();
		for (int i = 0; i < 100000; i++)
		{
			// We want to always leave alone the topmost part of the array until it has gone out of view
			int location = (bufferOffset + i) % 110000;
			//Randomize location
			float rdX = viewX + (random.nextFloat() * 2.0f - 1.0f) * (int) (Math2.mix(25, 15, rainIntensity));
			float rdY = viewY + (random.nextFloat() * 2.0f - 0.5f) * (int) (Math2.mix(20, 20, rainIntensity));
			float rdZ = viewZ + (random.nextFloat() * 2.0f - 1.0f) * (int) (Math2.mix(25, 15, rainIntensity));
			//Max height it can fall to before reverting to used
			float rdMh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates((int) rdX, (int) rdZ);
			//Raindrop size, change orientation to face viewer
			view2drop.x = rdX - viewX;
			view2drop.y = rdZ - viewZ;
			view2drop.normalise();
			float mx = 0.005f * -view2drop.y;
			float mz = 0.005f * view2drop.x;
			float rainDropletSize = 0.2f + random.nextFloat() * 0.18f;
			//Build triangle strip

			//00
			raindrops[location * 6 * 4 + 0 * 4 + 0] = rdX - mx;
			raindrops[location * 6 * 4 + 0 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 0 * 4 + 2] = rdZ - mz;
			raindrops[location * 6 * 4 + 0 * 4 + 3] = rdMh;
			//01
			raindrops[location * 6 * 4 + 1 * 4 + 0] = rdX - mx;
			raindrops[location * 6 * 4 + 1 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 1 * 4 + 2] = rdZ - mz;
			raindrops[location * 6 * 4 + 1 * 4 + 3] = rdMh + rainDropletSize;
			//10
			raindrops[location * 6 * 4 + 2 * 4 + 0] = rdX + mx;
			raindrops[location * 6 * 4 + 2 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 2 * 4 + 2] = rdZ + mz;
			raindrops[location * 6 * 4 + 2 * 4 + 3] = rdMh;
			//11
			raindrops[location * 6 * 4 + 3 * 4 + 0] = rdX - mx;
			raindrops[location * 6 * 4 + 3 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 3 * 4 + 2] = rdZ - mz;
			raindrops[location * 6 * 4 + 3 * 4 + 3] = rdMh + rainDropletSize;
			//01
			raindrops[location * 6 * 4 + 4 * 4 + 0] = rdX + mx;
			raindrops[location * 6 * 4 + 4 * 4 + 1] = rdY;
			raindrops[location * 6 * 4 + 4 * 4 + 2] = rdZ + mz;
			raindrops[location * 6 * 4 + 4 * 4 + 3] = rdMh;
			//00
			raindrops[location * 6 * 4 + 5 * 4 + 0] = rdX + mx;
			raindrops[location * 6 * 4 + 5 * 4 + 1] = rdY + rainDropletSize;
			raindrops[location * 6 * 4 + 5 * 4 + 2] = rdZ + mz;
			raindrops[location * 6 * 4 + 5 * 4 + 3] = rdMh + rainDropletSize;

		}
		raindropsData.clear();
		raindropsData.position(0);

		raindropsData.put(raindrops, 0, raindrops.length);
		raindropsData.flip();

		rainVerticesBuffer.uploadData(raindropsData);

		lastX = viewX;
		lastY = viewY;
		lastZ = viewZ;
	}

	long lastRender = 0L;

	private SoundSource rainSoundSource;

	// Rain falls at ~10m/s, so we prepare in advance 10 meters of rain to fall until we add some more on top
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.WeatherEffectsRenderer#renderEffects(io.xol.chunkstories.api.rendering.RenderingInterface)
	 */
	@Override
	public void renderEffects(RenderingInterface renderingContext)
	{
		viewX = (int) renderingContext.getCamera().getCameraPosition().getX();
		viewY = (int) renderingContext.getCamera().getCameraPosition().getY();
		viewZ = (int) renderingContext.getCamera().getCameraPosition().getZ();
		
		float rainPresence = world.getWeather();
		
		float snowPresence = getSnowPresence();
		
		rainPresence -= snowPresence;
		//rainPresence = Math2.clamp(rainPresence, 0f, 1f);
		
		if (rainPresence > 0.5)
		{
			renderRain(renderingContext, rainPresence);

			float rainIntensity = rainPresence * 2.0f - 1.0f;

			if (Client.getInstance().getClientSideController().getControlledEntity() != null)
			{
				int interiour = world.getSunlightLevelLocation(Client.getInstance().getClientSideController().getControlledEntity().getLocation());
				rainIntensity *= 0.2f + 0.8f * (interiour / 15f);

				rainIntensity *= 0.5;

				//System.out.println(rainIntensity);

				if (rainSoundSource == null || rainSoundSource.isDonePlaying())
					rainSoundSource = world.getSoundManager().playMusic("sounds/sfx/rainloop.ogg", 0, 0, 0, 1, rainIntensity, true);

				if (rainSoundSource != null)
					rainSoundSource.setGain(rainIntensity);
			}
			//rainSoundSource.setLooped(true);
		}
		else
		{
			if (rainSoundSource != null)
			{
				rainSoundSource.stop();
				rainSoundSource = null;
			}
		}
	}

	private float getSnowPresence()
	{
		Entity e = Client.getInstance().getClientSideController().getControlledEntity();
		if(e != null)
		{
			return world.getWeather()*Math2.clamp((e.getLocation().getY() - 20) / 20, 0, 1);
		}
		
		return 0;
	}

	//int vboId = -1;

	private void renderRain(RenderingInterface renderingContext, float rainPresence)
	{
		ShaderInterface weatherShader = renderingContext.useShader("weather");

		if ((System.currentTimeMillis() - lastRender) >= 1000 || Math.abs(viewX - lastX) > 10 || Math.abs(viewZ - lastZ) > 10)
		{
			generateRainForOneSecond(renderingContext, rainPresence);
			lastRender = System.currentTimeMillis();
		}

		renderingContext.setCullingMode(CullingMode.DISABLED);

		renderingContext.getCamera().setupShader(weatherShader);

		//Time since gen in ms
		weatherShader.setUniform1f("time", (System.currentTimeMillis() - lastRender) / 1000f);

		renderingContext.bindTexture2D("lightmap", TexturesHandler.getTexture("./textures/environement/lightcolors.png"));
		//weatherShader.setUniformSampler(0, "lightmap", TexturesHandler.getTexture("environement/lightcolors.png"));
		weatherShader.setUniform1f("sunTime", worldRenderer.getSky().time);

		renderingContext.bindAttribute("vertexIn", rainVerticesBuffer.asAttributeSource(VertexFormat.FLOAT, 4));

		float rainIntensity = Math.min(Math.max(0.0f, rainPresence - 0.5f) / 0.3f, 1.0f);

		//System.out.println("rainIntensity"+rainIntensity);
		
		renderingContext.draw(Primitive.TRIANGLE, 0, 20000 + (int) (90000 * rainIntensity));
		//GLCalls.drawArrays(GL_TRIANGLES, 0, 2000 + (int)(9000 * rainIntensity));
		//glDisable(GL_BLEND);

		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.WeatherEffectsRenderer#destroy()
	 */
	@Override
	public void destroy()
	{
		if (rainSoundSource != null)
		{
			rainSoundSource.stop();
			rainSoundSource = null;
		}

		rainVerticesBuffer.destroy();
	}

	@Override
	public void tick()
	{
		//Spawn some snow arround
		float snowPresence = getSnowPresence();
		
		Entity e = Client.getInstance().getClientSideController().getControlledEntity();
		if (e != null && world.getWorldRenderer() != null)
		{
			Location loc = e.getLocation();

			for (int i = 0; i < snowPresence * 10; i++)
				world.getParticlesManager().spawnParticleAtPosition("snow", loc.add(Math.random() * 20 - 10, Math.random() * 20, Math.random() * 20 - 10));
		}
	}
}
