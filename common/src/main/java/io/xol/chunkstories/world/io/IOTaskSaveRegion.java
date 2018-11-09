//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.region.RegionImplementation;

public class IOTaskSaveRegion extends IOTask {
	RegionImplementation region;

	public IOTaskSaveRegion(RegionImplementation holder) {
		this.region = holder;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		region.handler.savingOperations.incrementAndGet();

		// First compress all loaded chunks !
		region.compressAll();

		try {
			// Create the necessary directory structure if needed
			region.file.getParentFile().mkdirs();

			// Create the output stream
			FileOutputStream outputFileStream = new FileOutputStream(region.handler.file);
			DataOutputStream dos = new DataOutputStream(outputFileStream);

			region.handler.save(dos);

			outputFileStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Let go
		this.region.handler.savingOperations.decrementAndGet();

		synchronized (region.file) {
			region.file.notifyAll();
		}
		return true;
	}
}