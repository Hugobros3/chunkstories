package io.xol.chunkstories.api.rendering.vertex;

import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RecyclableByteBuffer {
	public ByteBuffer accessByteBuffer();

	public void recycle();
}