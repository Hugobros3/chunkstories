package xyz.chunkstories.graphics.opengl.shaders

import org.lwjgl.opengl.ARBDirectStateAccess
import org.lwjgl.opengl.GL32.*
import org.lwjgl.system.MemoryStack
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2D
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2DArray
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImageCubemap
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.opengl.FakePSO
import xyz.chunkstories.graphics.opengl.textures.OpenglOnionTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTextureCubemap

fun FakePSO.bindTexture(textureName: String, texture: OpenglTexture2D) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2D && it.name == textureName
    } as? GLSLUniformSampledImage2D ?: return

    ARBDirectStateAccess.glBindTextureUnit(resource.openglTextureUnit, texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnit)
}

fun FakePSO.bindTexture(textureName: String, texture: OpenglTextureCubemap) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImageCubemap && it.name == textureName
    } as? GLSLUniformSampledImageCubemap ?: return

    ARBDirectStateAccess.glBindTextureUnit(resource.openglTextureUnit, texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnit)
}

fun FakePSO.bindTexture(textureName: String, texture: OpenglOnionTexture2D) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2DArray && it.name == textureName
    } as? GLSLUniformSampledImage2DArray ?: return

    ARBDirectStateAccess.glBindTextureUnit(resource.openglTextureUnit, texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnit)
}

fun FakePSO.bindUBO(name: String, contents: InterfaceBlock) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformBlock && (it.name == name || it.rawName == name)
    } as? GLSLUniformBlock ?: return

    val buffer = ubos[resource]!!

    MemoryStack.stackPush()

    val fillMe = MemoryStack.stackMalloc(buffer.mapper.size)

    /*for (field in buffer.mapper.fields) {
        fillMe.position(field.offset)
        extractInterfaceBlockField(field, fillMe, contents)
    }*/
    extractInterfaceBlock(fillMe, 0, contents, buffer.mapper)

    fillMe.position(0)
    fillMe.limit(fillMe.capacity())
    buffer.upload(fillMe)

    MemoryStack.stackPop()

    glBindBufferRange(GL_UNIFORM_BUFFER, program.uboBindings[resource]!!, buffer.glId, 0, buffer.mapper.size.toLong())
}