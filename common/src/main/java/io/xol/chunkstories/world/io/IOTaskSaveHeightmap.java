//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.summary.HeightmapImplementation;

public class IOTaskSaveHeightmap extends IOTask {
	HeightmapImplementation heightmap;

	public IOTaskSaveHeightmap(HeightmapImplementation heightmap) {
		this.heightmap = heightmap;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		try {
			if (!heightmap.isLoaded())
				return true;

			heightmap.handler.getParentFile().mkdirs();
			if (!heightmap.handler.exists())
				heightmap.handler.createNewFile();
			FileOutputStream out = new FileOutputStream(heightmap.handler);

			int[] heights = heightmap.getHeightData();
			int[] ids = heightmap.getVoxelData();

			ByteBuffer writeMe = ByteBuffer.allocate(256 * 256 * 4);

			for (int i = 0; i < 256 * 256; i++)
				writeMe.putInt(heights[i]);

			byte[] compressed = HeightmapImplementation.compressor.compress(writeMe.array());

			int compressedSize = compressed.length;

			byte[] size = ByteBuffer.allocate(4).putInt(compressedSize).array();
			out.write(size);
			out.write(compressed);

			writeMe.clear();
			for (int i = 0; i < 256 * 256; i++)
				writeMe.putInt(ids[i]);

			compressed = HeightmapImplementation.compressor.compress(writeMe.array());
			compressedSize = compressed.length;

			size = ByteBuffer.allocate(4).putInt(compressedSize).array();
			out.write(size);
			out.write(compressed);

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}