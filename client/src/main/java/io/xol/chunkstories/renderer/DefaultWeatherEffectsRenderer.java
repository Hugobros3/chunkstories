package io.xol.chunkstories.renderer;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.vector.sp.Vector2fm;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldEffectsRenderer;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
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

	VertexBuffer rainVerticesBuffer = new VertexBufferGL();

	private void generateRainForOneSecond(RenderingInterface renderingContex, float rainPresence)
	{
		float rainIntensity = Math.min(Math.max(0.0f, rainPresence - 0.5f) / 0.3f, 1.0f);

		bufferOffset %= 110000;
		bufferOffset += 10000;
		Vector2fm view2drop = new Vector2fm();
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
			view2drop.setX(rdX - viewX);
			view2drop.setY(rdZ - viewZ);
			view2drop.normalize();
			float mx = 0.005f * -view2drop.getY();
			float mz = 0.005f * view2drop.getX();
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
		viewX = (int)(double) renderingContext.getCamera().getCameraPosition().getX();
		viewY = (int)(double) renderingContext.getCamera().getCameraPosition().getY();
		viewZ = (int)(double) renderingContext.getCamera().getCameraPosition().getZ();
		
		float rainPresence = world.getWeather();
		
		float snowPresence = getSnowPresence();
		
		rainPresence -= snowPresence;
		//rainPresence = Math2.clamp(rainPresence, 0f, 1f);
		
		if (rainPresence > 0.5)
		{
			renderRain(renderingContext, rainPresence);

			float rainIntensity = rainPresence * 2.0f - 1.0f;

			if (Client.getInstance().getPlayer().getControlledEntity() != null)
			{
				int interiour = world.getSunlightLevelLocation(Client.getInstance().getPlayer().getControlledEntity().getLocation());
				rainIntensity *= 0.2f + 0.8f * (interiour / 15f);

				rainIntensity *= 0.5;

				//Plays the rain loop

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
		Entity e = Client.getInstance().getPlayer().getControlledEntity();
		if(e != null)
		{
			return world.getWeather()*Math2.clamp((e.getLocation().getY() - 120) / 20, 0, 1);
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
		
		//TODO check on this
		weatherShader.setUniform1f("sunTime", world.getTime() % 10000);
		renderingContext.bindAttribute("vertexIn", rainVerticesBuffer.asAttributeSource(VertexFormat.FLOAT, 4));
		float rainIntensity = Math.min(Math.max(0.0f, rainPresence - 0.5f) / 0.3f, 1.0f);
		renderingContext.draw(Primitive.TRIANGLE, 0, 20000 + (int) (90000 * rainIntensity));
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

	long noThunderUntil;
	
	@Override
	public void tick()
	{
		//Spawn some snow arround
		float snowPresence = getSnowPresence();
		
		Entity e = Client.getInstance().getPlayer().getControlledEntity();
		if (e != null && world.getWorldRenderer() != null)
		{
			Location loc = e.getLocation();

			for (int i = 0; i < snowPresence * 10; i++)
				world.getParticlesManager().spawnParticleAtPosition("snow", loc.add(Math.random() * 20 - 10, Math.random() * 20, Math.random() * 20 - 10));
		}
		
		//Spawn a few lightning strikes once in a while
		//TODO server-side
		if(world.getWeather() > 0.9) {
			if(world.getWorldInfo().resolveProperty("thunderEnabled", "true").equals("true")) {
				//noThunderUntil = 0;
				if(System.currentTimeMillis() > noThunderUntil) {
					long maxDelay = Long.parseLong(world.getWorldInfo().resolveProperty("maxThunderDelayInMs", "30000"));
					long delay = Math.max(0, random.nextLong() % maxDelay);
				
					if(noThunderUntil != 0)
					{
						int worldX = random.nextInt(world.getSizeInChunks() * 32);
						int worldZ = random.nextInt(world.getSizeInChunks() * 32);
						
						//Spawn the strike
						System.out.println("clac");
						world.getParticlesManager().spawnParticleAtPosition("lightning_strike_illumination", new Location(world, worldX, 0, worldZ));
					}
					
					noThunderUntil = System.currentTimeMillis() + delay;
				}
			}
		}
	}
}
