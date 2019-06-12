//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import org.junit.Test

class ContentTests {

    @Test
    fun testAssimpMeshes() {
        val testContext = TestGameContext("")

        var m = testContext.content.models.getOrLoadModel("./models/human/human.dae")
        println("${m.meshes[0].vertices} lol:${m.javaClass}")

        println("${m.meshes.map { it.attributes.map { it.name } }}")

        // m = testContext.getContent().meshes().getMesh("./models/human.obj");
        // System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

        // m = testContext.getContent().meshes().getMesh("./models/human.dae");
        // System.out.println(m.getVerticesCount()+" lol:"+m.getClass());

        m = testContext.content.models.getOrLoadModel("./voxels/blockmodels/model_template.dae")
        println("${m.meshes[0].vertices} lol:${m.meshes[0].attributes[0].data}")

        println(m.meshes[0].material)
    }

    @Test
    fun testAssimpCube() {
        val testContext = TestGameContext("")

        val cubeModel = testContext.content.models.getOrLoadModel("voxels/blockmodels/cube.dae")
        for(mesh in cubeModel.meshes) {
            println(mesh)
        }
    }

    @Test
    fun testContentLoadByItself() {
        val testContext = TestGameContext("")
    }
}
