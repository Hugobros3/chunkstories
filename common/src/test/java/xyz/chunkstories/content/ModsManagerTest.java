//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import xyz.chunkstories.api.content.mods.ModsManager;
import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException;
import xyz.chunkstories.content.mods.ModsManagerImplementation;
import xyz.chunkstories.content.mods.ModsManagerImplementation.NonExistentCoreContent;

public class ModsManagerTest {

	@Test
	public void testModsManager() {
		String coreContentLocation = System.getProperty("coreContentLocation",
				"../chunkstories-core/build/distributions/core_content.zip");

		try {
			ModsManager modsManager = new ModsManagerImplementation(new File(coreContentLocation));
			modsManager.loadEnabledMods();
		} catch (NonExistentCoreContent e) {
			e.printStackTrace();
			fail();
		} catch (NotAllModsLoadedException e) {
			e.printStackTrace();
		}
	}
}
