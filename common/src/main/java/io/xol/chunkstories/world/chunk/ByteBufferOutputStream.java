//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** A simple helper class to deal with Java's stuck ways */
public class ByteBufferOutputStream extends OutputStream {

	final ByteBuffer outputBuffer;
	
	public ByteBufferOutputStream(ByteBuffer outputBuffer) {
		this.outputBuffer = outputBuffer;
	}

	@Override
	public void write(int b) throws IOException {
		outputBuffer.put((byte) b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		outputBuffer.put(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}
}
