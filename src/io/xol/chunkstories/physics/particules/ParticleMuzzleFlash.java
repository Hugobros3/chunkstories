package io.xol.chunkstories.physics.particules;

import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.renderer.lights.DefferedLight;
import static io.xol.chunkstories.physics.particules.Particle.Type.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleMuzzleFlash extends Particle
{
	int timer = 1;// for 40sec

	DefferedLight dl;

	@Override
	public Type getType()
	{
		return MUZZLE;
	}

	@Override
	public void update()
	{
		timer--;
		if(timer < 0)
			kill();
	}

	public ParticleMuzzleFlash(WorldInterface world, Vector3d vec)
	{
		this(world, vec.x, vec.y, vec.z);
	}
	
	public ParticleMuzzleFlash(WorldInterface world, double posX, double posY, double posZ)
	{
		super(world, posX, posY, posZ);
		dl = new DefferedLight(new Vector3f(1.0f, 181f/255f, 79/255f),
				new Vector3f((float) posX, (float) posY, (float) posZ),
				15f + (float) Math.random() * 5f);
		/*
		 * if(Math.random() > 0.5) { dl.angle = 22; float rotx = (float)
		 * (Math.random()*45f); float roty = (float) (Math.random()*360f); float
		 * transformedViewH = (float) ((rotx)/180*Math.PI);
		 * //System.out.println(Math.sin(transformedViewV)+"f"); dl.direction =
		 * new Vector3f((float)
		 * (Math.sin((-roty)/180*Math.PI)*Math.cos(transformedViewH)), (float)
		 * (Math.sin(transformedViewH)), (float)
		 * (Math.cos((-roty)/180*Math.PI)*Math.cos(transformedViewH))); dl.decay
		 * = 50f; }
		 */
	}

	@Override
	public String getTextureName()
	{
		return "./res/textures/particle.png";
	}

	@Override
	public boolean emitsLights()
	{
		return true;
	}

	@Override
	public Light getLightEmited()
	{
		return dl;
	}

	@Override
	public Float getSize()
	{
		return 0.0f;
	}
}
