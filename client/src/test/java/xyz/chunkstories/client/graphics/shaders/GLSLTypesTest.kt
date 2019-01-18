package xyz.chunkstories.client.graphics.shaders

import org.junit.Test
import xyz.chunkstories.graphics.common.shaders.GLSLType

class GLSLTypesTest {
    @Test
    fun testGlslBasicTypes() {
        for(type in GLSLType.BaseType.list) {
            println("type: ${type.classes.qualifiedName} <=> ${type.glslToken}")
        }
    }
}