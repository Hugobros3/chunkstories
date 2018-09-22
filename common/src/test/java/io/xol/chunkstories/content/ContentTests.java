//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content;

import io.xol.chunkstories.api.graphics.Mesh;
import org.junit.Test;

public class ContentTests {

	@Test
	public void testAssimpMeshes() {
		TestGameContext testContext = new TestGameContext(null);

		Mesh m = testContext.getContent().meshes().getMesh("./models/human/human.dae");
		System.out.println(m.getVertices() + " lol:" + m.getClass());

		// m = testContext.getContent().meshes().getMesh("./models/human.obj");
		// System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

		// m = testContext.getContent().meshes().getMesh("./models/human.dae");
		// System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

		m = testContext.getContent().meshes().getMesh("./voxels/blockmodels/model_template.dae");
		System.out.println(m.getVertices() + " lol:" + m.getClass());

		System.out.println(m.getMaterials());

		// System.out.println(m.getVertices().get(0)+":"+m.getVertices().get(1)+":"+m.getVertices().get(2));
		/*
		 * try { Asset a =
		 * testContext.getContent().getAsset("./models/human_all_animations.dae");
		 * //Asset a = testContext.getContent().getAsset("./animations/human/ded.bvh");
		 * 
		 * new NativeAssimpMesh(a, testContext.getContent().meshes()); new
		 * AssimpMeshLoader(a, testContext.getContent().meshes()); } catch
		 * (MeshLoadException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
	}

	@Test
	public void testContentLoadByItself() {
		TestGameContext testContext = new TestGameContext(null);
	}
}
