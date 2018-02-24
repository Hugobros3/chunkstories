//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.updater;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

public class VersionFileTests {
	
	@Test
	public void testVersionFileParsing() {
		Random random = new Random();
		String randomVersionNumber = random.nextInt(5) + "." + random.nextInt(12);
		String parseMe = "version: "+randomVersionNumber+"\n" + 
				"commit: 2db1bae811597872nicememe739bed139139cb5\n" + 
				"buildtime: 6-9-1996 23:48:36\n" ;

		VersionFile parsed = new VersionFile(parseMe);
		assertTrue(randomVersionNumber.equals(parsed.version));
		
		VersionFile parsedCrude = new VersionFile(randomVersionNumber);
		assertTrue(randomVersionNumber.equals(parsedCrude.version));
		
		//We make sure it can load the version.txt correctly ( and than it exists ! )
		File versionString = new File("version.txt");
		VersionFile loaded = VersionFile.loadFromFile(versionString);
		assertFalse(loaded.version.equals("unknown"));
		
		try {
			VersionFile.loadFromOnline();
		} catch(IOException e) {
			e.printStackTrace();
			fail();
		}
	}
}	
