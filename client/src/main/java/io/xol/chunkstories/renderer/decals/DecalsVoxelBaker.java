package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext.VoxelLighter;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsVoxelBaker implements VoxelBakerHighPoly, VoxelBakerCubic
{
	ByteBuffer byteBuffer;
	int cx, cy, cz;
	
	public DecalsVoxelBaker(ByteBuffer byteBuffer)
	{
		this.byteBuffer = byteBuffer;
	}
	
	public void setChunk(Chunk c) {
		cx = c.getChunkX();
		cy = c.getChunkY();
		cz = c.getChunkZ();
	}
	
	@Override
	public void addVerticeInt(int i0, int i1, int i2)
	{
		this.addVerticeFloat(i0, i1, i2);
	}

	@Override
	public void addVerticeFloat(float f0, float f1, float f2)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.putFloat(f0 + cx * 32);
		byteBuffer.putFloat(f1 + cy * 32);
		byteBuffer.putFloat(f2 + cz * 32);
	}

	@Override
	public void addTexCoordInt(int i0, int i1)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addColors(float[] t)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addColorsSpecial(float[] t, int extended)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addColors(float f0, float f1, float f2)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addColorsSpecial(float f0, float f1, float f2, int extended)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addNormalsInt(int i0, int i1, int i2, byte extra)
	{
		this.addVerticeFloat((i0 + 1) / 512 - 1, (i1 + 1) / 512 - 1, (i2 + 1) / 512 - 1);
	}

	@Override
	public void addColors(byte sunLight, byte blockLight, byte ao)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addColorsAuto(VoxelLighter voxelLighter, Corners corner)
	{
		// TODO Auto-generated method stub
		
	}

}
