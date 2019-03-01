//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import xyz.chunkstories.api.graphics.Mesh;
import org.junit.Test;
import xyz.chunkstories.api.graphics.representation.Model;

public class ContentTests {

    @Test
    public void testAssimpMeshes() {
        TestGameContext testContext = new TestGameContext("");

        Model m = testContext.getContent().getModels().getOrLoadModel("./models/human/human.dae");
        System.out.println(m.getMeshes().get(0).getVertices() + " lol:" + m.getClass());

        // m = testContext.getContent().meshes().getMesh("./models/human.obj");
        // System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

        // m = testContext.getContent().meshes().getMesh("./models/human.dae");
        // System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

        m = testContext.getContent().getModels().getOrLoadModel("./voxels/blockmodels/model_template.dae");
        System.out.println(m.getMeshes().get(0).getVertices() + " lol:" + m.getMeshes().get(0).getAttributes().get(0).getData());

        System.out.println(m.getMeshes().get(0).getMaterial());

        // System.out.println(m.getVertices().getVoxelComponent(0)+":"+m.getVertices().getVoxelComponent(1)+":"+m.getVertices().getVoxelComponent(2));
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
