package io.xol.chunkstories.content;

import org.junit.Test;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.mesh.AssimpMesh;
import io.xol.chunkstories.mesh.NativeAssimpMesh;

public class ContentTests {
	
	@Test
	public void testBasicContentInit() {
		TestGameContext testContext = new TestGameContext(null);
		
		try {
			Asset a = testContext.getContent().getAsset("./models/human_all_animations.dae");
			//Asset a = testContext.getContent().getAsset("./animations/human/ded.bvh");
			
			//new NativeAssimpMesh(a, testContext.getContent().meshes());
			new AssimpMesh(a, testContext.getContent().meshes());
		} catch (MeshLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
