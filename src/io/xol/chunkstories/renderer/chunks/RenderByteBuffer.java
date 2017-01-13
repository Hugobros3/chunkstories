package io.xol.chunkstories.renderer.chunks;

import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class to format information inside the byteBuffers
 */
public class RenderByteBuffer implements VoxelBaker
{
	protected ByteBuffer byteBuffer;
	
	public RenderByteBuffer(ByteBuffer byteBuffer)
	{
		this.byteBuffer = byteBuffer;
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addVerticeInt(int, int, int)
	 */
	@Override
	public void addVerticeInt(int i0, int i1, int i2)
	{
		if(i0 < 0 || i1 < 0 || i2 < 0)
			System.exit(0);
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) i0);
		byteBuffer.put((byte) i1);
		byteBuffer.put((byte) i2);
		byteBuffer.put((byte) 0x00);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addVerticeFloat(float, float, float)
	 */
	@Override
	public void addVerticeFloat(float f0, float f1, float f2)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.putFloat(f0);
		byteBuffer.putFloat(f1);
		byteBuffer.putFloat(f2);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addTexCoordInt(int, int)
	 */
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
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addColors(float[])
	 */
	@Override
	public void addColors(float[] t)
	{
		addColors(t[0], t[1], t[2]);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addColorsSpecial(float[], int)
	 */
	@Override
	public void addColorsSpecial(float[] t, int extended)
	{
		addColorsSpecial(t[0], t[1], t[2], extended);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addColors(float, float, float)
	 */
	@Override
	public void addColors(float f0, float f1, float f2)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) (f0 * 255));
		byteBuffer.put((byte) (f1 * 255));
		byteBuffer.put((byte) (f2 * 255));
		byteBuffer.put((byte) 0);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addColorsSpecial(float, float, float, int)
	 */
	@Override
	public void addColorsSpecial(float f0, float f1, float f2, int extended)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.put((byte) (f0 * 255));
		byteBuffer.put((byte) (f1 * 255));
		byteBuffer.put((byte) (f2 * 255));
		byteBuffer.put((byte) extended);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.VoxelBaker#addNormalsInt(int, int, int, boolean)
	 */
	@Override
	public void addNormalsInt(int i0, int i1, int i2, boolean wavy)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		int a = (i0) & 0x3FF;
		int b = ((i1) & 0x3FF) << 10;
		int c = ((i2) & 0x3FF) << 20;
		
		int d = (wavy ? 1 : 0) << 30;
		int kek = a | b | c | d;
		byteBuffer.putInt(kek);
	}
}
