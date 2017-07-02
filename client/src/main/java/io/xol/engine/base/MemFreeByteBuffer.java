package io.xol.engine.base;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A wrapper that automatically recycles JEmalloc-issued ByteBuffers from LWJGL3 */
public class MemFreeByteBuffer implements RecyclableByteBuffer {

	private final ByteBuffer byteBuffer;
	
	public MemFreeByteBuffer(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	@Override
	public ByteBuffer accessByteBuffer() {
		return byteBuffer;
	}

	@Override
	public void recycle() {
		MemoryUtil.memFree(byteBuffer);
	}

}
