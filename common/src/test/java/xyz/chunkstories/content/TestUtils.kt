//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import org.junit.Assert.fail

import org.junit.Test

import xyz.chunkstories.util.FoldersUtils

/**
 * Quick and dirty test with french curse words
 * it's not rude it's childish
 */
class TestUtils {
    @Test
    fun testPackpedalling() {
        assert(FoldersUtils.combineNames("./nique/du/saucisson/", "../../tamere.jpg") == "./nique/tamere.jpg")

        assert(FoldersUtils.combineNames("./nique/du/saucisson", "../../tamere.jpg") == "./tamere.jpg")

        try {
            assert(FoldersUtils.combineNames("./nique/du/saucisson/", "../../../../2deep4u.jpg") == "./nique/tamere.jpg")
            fail()
        } catch (e: IndexOutOfBoundsException) {
            //supposed to happen
        }

    }
}
