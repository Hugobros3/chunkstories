//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.storage;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import xyz.chunkstories.api.workers.TaskExecutor;
import xyz.chunkstories.world.io.IOTask;
import xyz.chunkstories.world.storage.RegionImplementation;

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
			//System.out.println("saved "+region.getFile());

			outputFileStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Let go
		this.region.getHandler().savingOperations.decrementAndGet();
		this.region.eventSavingFinishes();

		synchronized (region.getFile()) {
			region.getFile().notifyAll();
		}
		return true;
	}
}