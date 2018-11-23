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
import io.xol.chunkstories.world.storage.RegionImplementation;

public class IOTaskSaveRegion extends IOTask {
	RegionImplementation region;

	public IOTaskSaveRegion(RegionImplementation holder) {
		this.region = holder;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		region.getHandler().savingOperations.incrementAndGet();

		// First compress all loaded chunks !
		region.compressAll();

		try {
			// Create the necessary directory structure if needed
			region.getFile().getParentFile().mkdirs();

			// Create the output stream
			FileOutputStream outputFileStream = new FileOutputStream(region.getHandler().file);
			DataOutputStream dos = new DataOutputStream(outputFileStream);

			region.getHandler().save(dos);

			outputFileStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Let go
		this.region.getHandler().savingOperations.decrementAndGet();
		this.region.whenSavingDone();

		synchronized (region.getFile()) {
			region.getFile().notifyAll();
		}
		return true;
	}
}