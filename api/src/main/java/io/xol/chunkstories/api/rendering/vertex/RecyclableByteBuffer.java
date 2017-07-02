package io.xol.chunkstories.api.rendering.vertex;

import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Some ByteBuffer wrapped in a class that requires to be recycled after use
 * (for pooled or explicitly allocated memory )
 */
public interface RecyclableByteBuffer {
	public ByteBuffer accessByteBuffer();

	public void recycle();
}