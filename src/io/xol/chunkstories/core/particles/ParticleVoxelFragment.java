package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleVoxelFragment extends ParticleType
{
	public ParticleVoxelFragment(int id, String name)
	{
		super(id, name);
	}

	public class FragmentData extends ParticleData implements ParticleDataWithTextureCoordinates, ParticleDataWithVelocity {
		
		VoxelTexture tex;
		
		int timer = 1450; // 30s
		Vector3d vel = new Vector3d();
		
		float rightX, topY, leftX, bottomY;
		
		public FragmentData(float x, float y, float z, int data)
		{
			super(x, y, z);
			int id = VoxelFormat.id(data);
			
			tex = Voxels.get(id).getVoxelTexture(data, VoxelSides.LEFT, null);
			setData(data);
			//System.out.println("id+"+id + " "+ tex.atlasOffset / 32768f);
		}
		
		public void setVelocity(Vector3d vel)
		{
			this.vel = vel;
			this.add(vel.castToSimplePrecision());
		}

		@Override
		public float getTextureCoordinateXTopLeft()
		{
			return leftX;
			//return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXTopRight()
		{
			return rightX;
			//return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomLeft()
		{
			return leftX;
			//return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomRight()
		{
			return rightX;
			//return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYTopLeft()
		{
			return topY;
		}

		@Override
		public float getTextureCoordinateYTopRight()
		{
			return topY;
		}

		@Override
		public float getTextureCoordinateYBottomLeft()
		{
			return bottomY;
		}

		@Override
		public float getTextureCoordinateYBottomRight()
		{
			return bottomY;
		}

		void setData(int data)
		{
			int id = VoxelFormat.id(data);
			VoxelTexture tex = Voxels.get(id).getVoxelTexture(data, VoxelSides.LEFT, null);
			
			int qx = (int) Math.floor(Math.random() * 4.0);
			int rx = qx + 1;
			int qy = (int) Math.floor(Math.random() * 4.0);
			int ry = qy + 1;
			
			//System.out.println("qx:"+qx+"rx:"+rx);
			
			//leftX = (tex.atlasS + tex.atlasOffset) / 32768f;
			leftX = (tex.atlasS) / 32768f + tex.atlasOffset / 32768f * (qx / 4.0f);
			rightX = (tex.atlasS) / 32768f + tex.atlasOffset / 32768f * (rx / 4.0f);
			

			topY = (tex.atlasT) / 32768f + tex.atlasOffset / 32768f * (qy / 4.0f);
			bottomY = (tex.atlasT) / 32768f + tex.atlasOffset / 32768f * (ry / 4.0f);
			
			//topY = (tex.atlasT + tex.atlasOffset) / 32768f;
			//bottomY = (tex.atlasT) / 32768f;
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
		return TexturesHandler.getTexture("./textures/tiles_merged_albedo.png");
	}

	@Override
	public float getBillboardSize()
	{
		return 0.125f / 2.0f;
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
		b.setX((float) (b.getX() + b.vel.getX()));
		b.setY((float) (b.getY() + b.vel.getY()));
		b.setZ((float) (b.getZ() + b.vel.getZ()));
		
		if (!((WorldImplementation) world).checkCollisionPoint(b.getX(), b.getY() - 0.1, b.getZ()))
			b.vel.setY(b.vel.getY() + -0.89/60.0);
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
