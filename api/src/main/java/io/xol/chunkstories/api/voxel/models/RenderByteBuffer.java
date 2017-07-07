package io.xol.chunkstories.api.voxel.models;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext.VoxelLighter;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class to format information inside the byteBuffers
 */
public class RenderByteBuffer implements VoxelBakerHighPoly, VoxelBakerCubic
{
	protected ByteBuffer byteBuffer;
	
	public RenderByteBuffer(ByteBuffer byteBuffer)
	{
		this.byteBuffer = byteBuffer;
	}
	
	@Override
	public void addVerticeInt(int i0, int i1, int i2)
	{
		if(i0 < 0 || i1 < 0 || i2 < 0) {
			System.out.println("screw off");
			System.exit(0);
		}
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) i0);
		byteBuffer.put((byte) i1);
		byteBuffer.put((byte) i2);
		byteBuffer.put((byte) 0x00);
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
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) ((i0) & 0xFF));
		byteBuffer.put((byte) ((i0 >> 8) & 0xFF));
		byteBuffer.put((byte) ((i1) & 0xFF));
		byteBuffer.put((byte) ((i1 >> 8) & 0xFF));
	}
	
	@Override
	public void addColors(float[] t)
	{
		addColors(t[0], t[1], t[2]);
	}

	@Override
	public void addColors(byte sunLight, byte blockLight, byte ao)
	{
		byteBuffer.put(blockLight);
		byteBuffer.put(sunLight);
		byteBuffer.put(ao);
		byteBuffer.put((byte) 0);
	}

	@Override
	public void addColorsAuto(VoxelLighter voxelLighter, Corners corner)
	{
		addColors(voxelLighter.getSunlightLevelForCorner(corner), voxelLighter.getBlocklightLevelForCorner(corner), voxelLighter.getAoLevelForCorner(corner));
	}
	
	@Override
	public void addColorsSpecial(float[] t, int extended)
	{
		addColorsSpecial(t[0], t[1], t[2], extended);
	}
	
	@Override
	public void addColors(float f0, float f1, float f2)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) (f0));
		byteBuffer.put((byte) (f1));
		byteBuffer.put((byte) (f2));
		byteBuffer.put((byte) 0);
	}
	
	@Override
	public void addColorsSpecial(float f0, float f1, float f2, int extended)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) (f0));
		byteBuffer.put((byte) (f1));
		byteBuffer.put((byte) (f2));
		byteBuffer.put((byte) extended);
	}
	
	@Override
	public void addNormalsInt(int i0, int i1, int i2, byte extra)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		int a = (i0) & 0x3FF;
		int b = ((i1) & 0x3FF) << 10;
		int c = ((i2) & 0x3FF) << 20;
		
		int d = (extra & 0x3) << 30;
		int kek = a | b | c | d;
		byteBuffer.putInt(kek);
	}
}
