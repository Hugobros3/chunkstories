package io.xol.chunkstories.content;

import org.junit.Test;

import io.xol.chunkstories.api.mesh.Mesh;

public class ContentTests {
	
	@Test
	public void testBasicContentInit() {
		TestGameContext testContext = new TestGameContext(null);
		
		Mesh m = testContext.getContent().meshes().getMesh("./models/human_all_animations.dae");
		System.out.println(m.getVerticesCount()+" lol:"+m.getClass());
		
		m = testContext.getContent().meshes().getMesh("./models/human.obj");
		System.out.println(m.getVerticesCount()+" lol:"+m.getClass());
		
		m = testContext.getContent().meshes().getMesh("./models/human.dae");
		System.out.println(m.getVerticesCount()+" lol:"+m.getClass());
		
		System.out.println(m.getVertices().get(0)+":"+m.getVertices().get(1)+":"+m.getVertices().get(2));
		/*try {
			Asset a = testContext.getContent().getAsset("./models/human_all_animations.dae");
			//Asset a = testContext.getContent().getAsset("./animations/human/ded.bvh");
			
			new NativeAssimpMesh(a, testContext.getContent().meshes());
			new AssimpMeshLoader(a, testContext.getContent().meshes());
		} catch (MeshLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
