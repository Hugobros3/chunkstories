package io.xol.chunkstories.physics.particules;

import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.world.World;
import static io.xol.chunkstories.physics.particules.Particle.Type.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleSetupLight extends Particle
{

	int timer = 4800;// for 40sec

	DefferedLight dl;

	public Type getType()
	{
		return SLIGHT;
	}

	public void update()
	{

	}

	public ParticleSetupLight(World world, double posX, double posY,
			double posZ, DefferedLight dl)
	{
		super(world, posX, posY, posZ);
		this.dl = dl;
	}

	@Override
	public String getTextureName()
	{
		return "./res/textures/light.png";
	}

	public boolean emitsLights()
	{
		return true;
	}

	public DefferedLight getLightEmited()
	{
		return dl;
	}

	@Override
	public Float getSize()
	{
		return 3f;
	}
}
