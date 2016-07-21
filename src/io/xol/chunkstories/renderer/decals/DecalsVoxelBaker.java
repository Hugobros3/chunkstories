package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;

import io.xol.chunkstories.renderer.chunks.VoxelBaker;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsVoxelBaker implements VoxelBaker
{
	ByteBuffer byteBuffer;
	
	public DecalsVoxelBaker(ByteBuffer byteBuffer)
	{
		this.byteBuffer = byteBuffer;
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
		byteBuffer.putFloat(f0);
		byteBuffer.putFloat(f1);
		byteBuffer.putFloat(f2);
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
	public void addNormalsInt(int i0, int i1, int i2, boolean wavy)
	{
		this.addVerticeFloat((i0 + 1) / 512 - 1, (i1 + 1) / 512 - 1, (i2 + 1) / 512 - 1);
	}

}
