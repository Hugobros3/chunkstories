//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.world.generator.TaskGenerateWorldThinSlice;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class IOTaskLoadHeightmap extends IOTask {
	private static final Logger logger = LoggerFactory.getLogger("world.io");

	private final boolean shouldGenerateMap;
	
	protected LZ4Factory factory = LZ4Factory.fastestInstance();
	protected LZ4FastDecompressor decompressor = factory.fastDecompressor();

	HeightmapImplementation heightmap;
	
	TaskGenerateWorldThinSlice generationTask;

	public IOTaskLoadHeightmap(HeightmapImplementation heightmap) {
		this.heightmap = heightmap;
		
		this.shouldGenerateMap = !heightmap.handler.exists();
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		if (heightmap.isLoaded())
			return true;
		
		if (!shouldGenerateMap) {
			
			try {
				FileInputStream in = new FileInputStream(heightmap.handler);

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

				heightmap.setData(heights, ids);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Just blank out the stuff so the real job can be done
			int h, t;

			int[] heights = new int[256 * 256];
			int[] ids = new int[256 * 256];

			t = 0;
			h = Heightmap.NO_DATA;
			for (int x = 0; x < 256; x++)
				for (int z = 0; z < 256; z++) {
					heights[x * 256 + z] = h;
					ids[x * 256 + z] = t;
				}

			heightmap.setData(heights, ids);
			
			/*for(int relative_chunkX = 0; relative_chunkX < 8; relative_chunkX++) {
				CompoundFence f = new CompoundFence();
				for(int relative_chunkZ = 0; relative_chunkZ < 8; relative_chunkZ++) {
					TaskGenerateWorldThinSlice task = new TaskGenerateWorldThinSlice(heightmap.world, heightmap.getRegionX() * 8 + relative_chunkX,heightmap.getRegionZ() * 8 + relative_chunkZ, heightmap);
					heightmap.world.getGameContext().tasks().scheduleTask(task);
					f.add(task);
				}
				f.traverse();
			}*/
		}
		return true;
	}
}