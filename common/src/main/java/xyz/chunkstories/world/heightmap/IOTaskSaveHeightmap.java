//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.heightmap;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import xyz.chunkstories.api.workers.TaskExecutor;
import xyz.chunkstories.api.world.heightmap.Heightmap;
import xyz.chunkstories.world.heightmap.HeightmapImplementation;
import xyz.chunkstories.world.io.IOTask;

public class IOTaskSaveHeightmap extends IOTask {
	HeightmapImplementation heightmap;

	public IOTaskSaveHeightmap(HeightmapImplementation heightmap) {
		this.heightmap = heightmap;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		try {
			if (!(heightmap.getState() instanceof Heightmap.State.Saving))
				throw new RuntimeException("Illegal state: You can't save a heightmap not in the saving state !");

			heightmap.getFile().getParentFile().mkdirs();
			if (!heightmap.getFile().exists())
				heightmap.getFile().createNewFile();
			FileOutputStream out = new FileOutputStream(heightmap.getFile());

			int[] heights = heightmap.getHeightData();
			int[] ids = heightmap.getVoxelData();

			ByteBuffer writeMe = ByteBuffer.allocate(256 * 256 * 4);

			for (int i = 0; i < 256 * 256; i++)
				writeMe.putInt(heights[i]);

			byte[] compressed = HeightmapImplementation.Companion.getCompressor().compress(writeMe.array());

			int compressedSize = compressed.length;

			byte[] size = ByteBuffer.allocate(4).putInt(compressedSize).array();
			out.write(size);
			out.write(compressed);

			writeMe.clear();
			for (int i = 0; i < 256 * 256; i++)
				writeMe.putInt(ids[i]);

			compressed = HeightmapImplementation.Companion.getCompressor().compress(writeMe.array());
			compressedSize = compressed.length;

			size = ByteBuffer.allocate(4).putInt(compressedSize).array();
			out.write(size);
			out.write(compressed);

			out.close();

			//System.out.println("wrote "+heightmap.getFile());
		} catch (Exception e) {
			e.printStackTrace();
		}

		//System.out.println("heightmap SAVED");

		heightmap.eventSavingFinished();
		return true;
	}
}