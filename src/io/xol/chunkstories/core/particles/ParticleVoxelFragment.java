package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleVoxelFragment extends ParticleType
{
	public ParticleVoxelFragment(int id, String name)
	{
		super(id, name);
	}

	public class FragmentData extends ParticleData implements ParticleTextureCoordinates {
		
		VoxelTexture tex;
		
		int timer = 60 * 30; // 30s
		Vector3d vel = new Vector3d();
		
		public FragmentData(float x, float y, float z, int data)
		{
			super(x, y, z);
			int id = VoxelFormat.id(data);
			
			tex = VoxelTypes.get(id).getVoxelTexture(data, 0, null);
			//System.out.println("id+"+id + " "+ tex.atlasOffset / 32768f);
		}
		
		public void setVelocity(Vector3d vel)
		{
			this.vel = vel;
		}

		@Override
		public float getTextureCoordinateXTopLeft()
		{
			return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXTopRight()
		{
			// TODO Auto-generated method stub
			return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomLeft()
		{
			// TODO Auto-generated method stub
			return tex.atlasS / 32768f; 
		}

		@Override
		public float getTextureCoordinateXBottomRight()
		{
			// TODO Auto-generated method stub
			return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYTopLeft()
		{
			// TODO Auto-generated method stub
			return (tex.atlasT + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYTopRight()
		{
			// TODO Auto-generated method stub
			return (tex.atlasT + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYBottomLeft()
		{
			// TODO Auto-generated method stub
			return tex.atlasT / 32768f;
		}

		@Override
		public float getTextureCoordinateYBottomRight()
		{
			// TODO Auto-generated method stub
			return tex.atlasT / 32768f;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new FragmentData(x, y, z, world.getVoxelData((int)x, (int)y, (int)z));
	}

	@Override
	public Texture2D getTexture()
	{
		return TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.125f;
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		FragmentData b = (FragmentData) data;
		
		b.timer--;
		b.x += b.vel.x;
		b.y += b.vel.y;
		b.z += b.vel.z;
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.x, b.y, b.z))
			b.vel.y += -0.89/60.0;
		else
			b.vel.zero();
		
		// 60th square of 0.5
		b.vel.scale(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.zero();
		
		if(b.timer < 0)
			b.destroy();
	}
}
