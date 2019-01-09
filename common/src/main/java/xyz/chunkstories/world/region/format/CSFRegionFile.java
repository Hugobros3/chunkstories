//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.region.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.world.serialization.OfflineSerializedData;
import xyz.chunkstories.world.storage.RegionImplementation;

/** Common region file storage formats stuff */
public abstract class CSFRegionFile implements OfflineSerializedData {

	public final RegionImplementation region;
	public final File file;

	private static final Logger logger = LoggerFactory.getLogger("world.serialization.region");

	public static Logger logger() {
		return logger;
	}

	public CSFRegionFile(RegionImplementation holder, File file) {
		this.region = holder;
		this.file = file;
	}

	public AtomicInteger savingOperations = new AtomicInteger();

	public boolean exists() {
		return file.exists();
	}

	public abstract void load(DataInputStream in) throws IOException;

	public abstract void save(DataOutputStream out) throws IOException;

	public void finishSavingOperations() {
		// Waits out saving operations.
		while (savingOperations.get() > 0)
			// System.out.println(savingOperations.getVoxelComponent());
			synchronized (this) {
				try {
					wait(20L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	}

	public static CSFRegionFile determineVersionAndCreate(RegionImplementation region) {

		if (region.getFile().exists()) {

			try {
				FileInputStream fist = new FileInputStream(region.getFile());
				DataInputStream in = new DataInputStream(fist);

				// Read jsut the first 12 bytes of data

				long magicNumber = in.readLong();

				int versionNumber = in.readInt();
				// int writeTimestamp = in.readInt();

				in.close();

				// chnkstrs in ascii
				if (magicNumber == 6003953969960732739L) {
					if (versionNumber == 0x2D)
						return new CSFRegionFile0x2D(region, region.getFile());
					else
						throw new RuntimeException("Unhandled file format revision: " + versionNumber);
				}

			} catch (IOException e) {

			}
		} else
			return new CSFRegionFile0x2D(region, region.getFile());

		System.out.println(region.getFile());
		System.out.println(region.getFile().exists());
		Thread.dumpStack();

		throw new RuntimeException("Older unsupported file format detected!");
	}

}