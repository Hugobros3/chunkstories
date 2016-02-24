package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class to format information inside the byteBuffers
 * @author gobrosse
 *
 */
public class RenderByteBuffer
{
	ByteBuffer byteBuffer;
	
	public RenderByteBuffer(ByteBuffer byteBuffer)
	{
		this.byteBuffer = byteBuffer;
	}
	
	public void addVerticeInt(int i0, int i1, int i2)
	{
		int a = (int) (i0) & 0x3FF;
		int b = ((int) (i1) & 0x3FF) << 10;
		int c = ((int) (i2) & 0x3FF) << 20;
		int kek = a | b | c;
		byteBuffer.putInt(kek);
	}
	
	public void addTexCoordInt(int i0, int i1)
	{
		byteBuffer.put((byte) ((i0) & 0xFF));
		byteBuffer.put((byte) ((i0 >> 8) & 0xFF));
		byteBuffer.put((byte) ((i1) & 0xFF));
		byteBuffer.put((byte) ((i1 >> 8) & 0xFF));
	}
	
	public void addColors(float[] t)
	{
		addColors(t[0], t[1], t[2]);
	}
	
	public void addColors(float f0, float f1, float f2)
	{
		byteBuffer.put((byte) (f0 * 255));
		byteBuffer.put((byte) (f1 * 255));
		byteBuffer.put((byte) (f2 * 255));
		byteBuffer.put((byte) 0);
	}
	
	public void addNormalsInt(int i0, int i1, int i2, boolean wavy)
	{
		int a = (int) (i0) & 0x3FF;
		int b = ((int) (i1) & 0x3FF) << 10;
		int c = ((int) (i2) & 0x3FF) << 20;
		
		int d = (wavy ? 1 : 0) << 30;
		int kek = a | b | c | d;
		byteBuffer.putInt(kek);
	}
}
