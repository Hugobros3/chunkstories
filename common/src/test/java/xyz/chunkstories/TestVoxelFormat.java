//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories;

import java.util.Random;

import org.junit.Test;

import static xyz.chunkstories.block.VoxelFormat.*;
import static org.junit.Assert.*;

/** Ensures that VoxelFormat doesn't specify nonsense */
public class TestVoxelFormat {

	@Test
	public void testVoxelFormat() {
		// Demo-debug
		int data = format(65535, 255, 0, 0);
		data = changeId(data, 15);
		data = changeSunlight(data, 3);
		data = changeBlocklight(data, 7);

		System.out.println(data);
		System.out.println("BlockID : " + id(data) + " Meta : " + meta(data) + " Sun : " + sunlight(data) + " Block : " + blocklight(data));

		Random random = new Random();
		int tests = 1000000;
		for (int test = 0; test < tests; test++) {
			int blockId = random.nextInt(65536);
			int blockLight = random.nextInt(16);
			int sunLight = random.nextInt(16);
			int metaData = random.nextInt(256);

			int formatted = format(blockId, metaData, sunLight, blockLight);
			if (blockId == id(formatted) && blockLight == blocklight(formatted) && sunLight == sunlight(formatted) && metaData == meta(formatted)) {
				// Ok good
			} else {
				System.out.println(formatted);
				System.out.println(blockId + " vs " + id(formatted));
				System.out.println(blockLight + " vs " + blocklight(formatted));
				System.out.println(sunLight + " vs " + sunlight(formatted));
				System.out.println(metaData + " vs " + meta(formatted));
				fail();
				// throw new RuntimeException("Test failed.");
			}

			int blockId2 = random.nextInt(65536);
			int blockLight2 = random.nextInt(16);
			int sunLight2 = random.nextInt(16);
			int metaData2 = random.nextInt(256);

			int blockIdExpected = blockId;
			int blockLightExpected = blockLight;
			int sunLightExpected = sunLight;
			int metaDataExpected = metaData;

			blockIdExpected = blockId2;
			formatted = changeId(formatted, blockIdExpected);

			if (!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted)
					&& metaDataExpected == meta(formatted)))
				fail();

			metaDataExpected = metaData2;
			formatted = changeMeta(formatted, metaDataExpected);

			if (!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted)
					&& metaDataExpected == meta(formatted)))
				fail();

			sunLightExpected = sunLight2;
			formatted = changeSunlight(formatted, sunLightExpected);

			if (!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted)
					&& metaDataExpected == meta(formatted)))
				fail();

			blockLightExpected = blockLight2;
			formatted = changeBlocklight(formatted, blockLightExpected);

			if (!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted)
					&& metaDataExpected == meta(formatted)))
				fail();
		}

		System.out.println("Ran through " + tests + " runs of testing just fine.");
	}
}
