//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IOTaskLoadHeightmap extends IOTask {
	private static final Logger logger = LoggerFactory.getLogger("world.io");

	private final boolean isThereActuallyData;

	protected LZ4Factory factory = LZ4Factory.fastestInstance();
	protected LZ4FastDecompressor decompressor = factory.fastDecompressor();

	private HeightmapImplementation heightmap;

	public IOTaskLoadHeightmap(HeightmapImplementation heightmap) {
		this.heightmap = heightmap;

		this.isThereActuallyData = !heightmap.getFile().exists();
	}

	@Override public boolean task(TaskExecutor taskExecutor) {
		if (!isThereActuallyData) {
			try {
				FileInputStream in = new FileInputStream(heightmap.getFile());

				byte[] size = new byte[4];

				int[] heights = new int[256 * 256];
				int[] ids = new int[256 * 256];

				try {
					in.read(size);
					int s = ByteBuffer.wrap(size).asIntBuffer().get(0);
					byte[] compressed = new byte[s];
					in.read(compressed);

					byte[] decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
					IntBuffer ib = ByteBuffer.wrap(decompressed).asIntBuffer();
					for (int i = 0; i < 256 * 256; i++)
						heights[i] = ib.get();

					in.read(size);
					s = ByteBuffer.wrap(size).asIntBuffer().get(0);
					compressed = new byte[s];
					in.read(compressed);

					decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
					ib = ByteBuffer.wrap(decompressed).asIntBuffer();
					for (int i = 0; i < 256 * 256; i++)
						ids[i] = ib.get();

					in.close();
				} catch (Exception e) {
					logger.error("Could not load load chunk summary at " + heightmap + " cause: " + e.getMessage());
				}

				heightmap.whenDataLoadedCallback(heights, ids);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Just blank out the stuff so the real job can be done
			int h, t;

			int[] heights = new int[256 * 256];
			int[] ids = new int[256 * 256];

			t = 0;
			h = Heightmap.Companion.getNO_DATA();
			for (int x = 0; x < 256; x++)
				for (int z = 0; z < 256; z++) {
					heights[x * 256 + z] = h;
					ids[x * 256 + z] = t;
				}

			heightmap.whenDataLoadedCallback(heights, ids);
		}
		return true;
	}
}