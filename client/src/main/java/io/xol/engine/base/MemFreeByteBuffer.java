//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.base;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;

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
