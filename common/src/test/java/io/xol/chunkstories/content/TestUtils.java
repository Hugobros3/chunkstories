package io.xol.chunkstories.content;

import static org.junit.Assert.fail;

import org.junit.Test;

import io.xol.chunkstories.util.FoldersUtils;

/** Quick and dirty test with french curse words */
public class TestUtils {
	@Test
	public void testPackpedalling() {
		assert FoldersUtils.combineNames("./nique/du/saucisson/", "../../tamere.jpg").equals("./nique/tamere.jpg");
		
		assert FoldersUtils.combineNames("./nique/du/saucisson", "../../tamere.jpg").equals("./tamere.jpg");
		
		try {
			assert FoldersUtils.combineNames("./nique/du/saucisson/", "../../../../2deep4u.jpg").equals("./nique/tamere.jpg");
			fail();
		} catch(IndexOutOfBoundsException e) {
			
		}
	}
}
