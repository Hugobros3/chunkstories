//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import xyz.chunkstories.api.content.Asset;

public class AssetToByteBufferHelper {

	public static ByteBuffer loadIntoByteBuffer(Asset asset) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			InputStream is = asset.read();
			byte[] buffer = new byte[4096];
			while (is.available() > 0) {
				int r = is.read(buffer);
				baos.write(buffer, 0, r);
			}
			is.close();

			byte[] bytes = baos.toByteArray();

			ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length);
			bb.put(bytes);
			bb.flip();

			return bb;
		} catch (IOException e) {
			throw new RuntimeException("Couldn't fully read asset");
		}
	}
}
