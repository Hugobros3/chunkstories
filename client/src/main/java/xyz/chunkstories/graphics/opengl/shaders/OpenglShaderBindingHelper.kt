package xyz.chunkstories.graphics.opengl.shaders

import org.joml.Matrix4f
import org.lwjgl.opengl.ARBDirectStateAccess.glBindTextureUnit
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2D
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2DArray
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImageCubemap
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.opengl.FakePSO
import xyz.chunkstories.graphics.opengl.resources.OpenglSampler
import xyz.chunkstories.graphics.opengl.textures.OpenglOnionTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTextureCubemap

fun FakePSO.bindTexture(textureName: String, index: Int = 0, texture: OpenglTexture2D, sampler: OpenglSampler?) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2D && it.name == textureName
    } as? GLSLUniformSampledImage2D ?: return

    val tetureUnit = resource.openglTextureUnits[index]

    glBindTextureUnit(tetureUnit, texture.glTexId)
    if(sampler != null) {
        glBindSampler(tetureUnit, sampler.glId)
    }

    val uniformLocation = if (resource.count > 1)
        glGetUniformLocation(program.programId, resource.name+"[$index]")
    else
        glGetUniformLocation(program.programId, resource.name)

    glUniform1i(uniformLocation, tetureUnit)
}

fun FakePSO.bindTexture(textureName: String, texture: OpenglTextureCubemap) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImageCubemap && it.name == textureName
    } as? GLSLUniformSampledImageCubemap ?: return

    glBindTextureUnit(resource.openglTextureUnits[0], texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnits[0])
}

fun FakePSO.bindTexture(textureName: String, texture: OpenglOnionTexture2D) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2DArray && it.name == textureName
    } as? GLSLUniformSampledImage2DArray ?: return

    glBindTextureUnit(resource.openglTextureUnits[0], texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnits[0])
}

fun FakePSO.bindUBO(name: String, contents: InterfaceBlock) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformBlock && (it.name == name || it.rawName == name)
    } as? GLSLUniformBlock ?: return

    val buffer = ubos[resource]!!

    MemoryStack.stackPush()

    val fillMe = MemoryStack.stackMalloc(buffer.mapper.size)

    var modifiedContents = contents

    if(name == "camera" && contents is Camera) {
        //All the matrix math in the game assumes Vulkan conventions, but OpenGL has different ones
        //This hacks up the projection matrix to get the game renderer to use the full depth range (-1, 1) even though it assumes (0, 1) depth
        //We leave the projctionInverse alone because it will get (0,1) depth range from reading the zbuffer already
        //Notes:
        //In OpenGL the clip-space is ((-1,-1,-1) to (1,1,1)) and texture-space ((0,0,0) to (1,1,1))
        //In Vulkan the clip-space is ((-1,-1,0) to (1,1,1)) and so we don't bother touching the Z coordinate

        val hackedProjectionMatrix = Matrix4f(contents.projectionMatrix)

        val modifierMatrix = Matrix4f()
        modifierMatrix.translate(0f, 0f, -1f)
        modifierMatrix.scale(1f, 1f, 2f)
        modifierMatrix.mul(hackedProjectionMatrix, hackedProjectionMatrix)

        modifiedContents = Camera(contents.position, contents.lookingAt, contents.up, contents.fov, contents.viewMatrix, hackedProjectionMatrix, contents.normalMatrix, contents.viewMatrixInverted, contents.projectionMatrixInverted)
    }

    /*for (field in buffer.mapper.fields) {
        fillMe.position(field.offset)
        extractInterfaceBlockField(field, fillMe, contents)
    }*/
    extractInterfaceBlock(fillMe, 0, modifiedContents, buffer.mapper)

    fillMe.position(0)
    fillMe.limit(fillMe.capacity())
    buffer.upload(fillMe)

    MemoryStack.stackPop()

    glBindBufferRange(GL_UNIFORM_BUFFER, program.uboBindings[resource]!!, buffer.glId, 0, buffer.mapper.size.toLong())
}