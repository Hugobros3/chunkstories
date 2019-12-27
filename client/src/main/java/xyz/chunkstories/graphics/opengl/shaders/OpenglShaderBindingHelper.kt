package xyz.chunkstories.graphics.opengl.shaders

import org.joml.Matrix4f
import org.lwjgl.opengl.ARBDirectStateAccess.glBindTextureUnit
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.opengl.FakePSO
import xyz.chunkstories.graphics.opengl.buffers.OpenglUniformBuffer
import xyz.chunkstories.graphics.opengl.resources.OpenglSampler
import xyz.chunkstories.graphics.opengl.textures.OpenglOnionTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D
import xyz.chunkstories.graphics.opengl.textures.OpenglTextureCubemap

private fun FakePSO.bindTextureUnit(unit: Int, textureId: Int, type: Int) {
    if(backend.openglSupport.dsaSupport) {
        glBindTextureUnit(unit, textureId)
    } else {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(type, textureId)
    }
}

fun FakePSO.bindTexture(textureName: String, index: Int = 0, texture: OpenglTexture2D, sampler: OpenglSampler?) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2D && it.name == textureName
    } as? GLSLUniformSampledImage2D ?: return

    val tetureUnit = resource.openglTextureUnits[index]

    //glBindTextureUnit(tetureUnit, texture.glTexId)
    bindTextureUnit(tetureUnit, texture.glTexId, GL_TEXTURE_2D)
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

    //glBindTextureUnit(resource.openglTextureUnits[0], texture.glTexId)
    bindTextureUnit(resource.openglTextureUnits[0], texture.glTexId, GL_TEXTURE_CUBE_MAP)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnits[0])
}

fun FakePSO.bindTexture(textureName: String, texture: OpenglOnionTexture2D) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2DArray && it.name == textureName
    } as? GLSLUniformSampledImage2DArray ?: return

    //glBindTextureUnit(resource.openglTextureUnits[0], texture.glTexId)
    bindTextureUnit(resource.openglTextureUnits[0], texture.glTexId, GL_TEXTURE_2D_ARRAY)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnits[0])
}


fun FakePSO.bindStructuredUBO(name: String, contents: InterfaceBlock) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformBlock && (it.instanceName == name)
    } as? GLSLUniformBlock ?: throw Exception("Couldn't find structured ubo $name")

    bindUBO(resource, contents)
}

fun FakePSO.bindUBO(uniformBlock: GLSLUniformBlock, contents: InterfaceBlock) {
    val buffer = ubos[uniformBlock]!!

    MemoryStack.stackPush()
        val cpuTempBuffer = MemoryStack.stackMalloc(buffer.mapper.size)

        var actualContentToUpload = contents

        if(uniformBlock.instanceName == "camera" && contents is Camera) {
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

            actualContentToUpload = Camera(contents.position, contents.lookingAt, contents.up, contents.fov, contents.viewMatrix, hackedProjectionMatrix, contents.normalMatrix, contents.viewMatrixInverted, contents.projectionMatrixInverted)
        }

        extractInterfaceBlock(cpuTempBuffer, 0, actualContentToUpload, buffer.mapper)

        cpuTempBuffer.position(0)
        cpuTempBuffer.limit(cpuTempBuffer.capacity())
        buffer.upload(cpuTempBuffer)
    MemoryStack.stackPop()

    bindUBO(uniformBlock, buffer)
}

fun FakePSO.bindUBO(uniformBlock: GLSLUniformBlock, buffer: OpenglUniformBuffer, offset: Long = 0) {
    glBindBufferRange(GL_UNIFORM_BUFFER, program.uboBindings[uniformBlock]!!, buffer.glId, offset, buffer.mapper.size.toLong())
}

@Deprecated("depends on the current flaky implementation of instanced inputs as per-drawcall modified ubos")
fun FakePSO.bindInstancedInput(instancedInput: GLSLInstancedInput, contents: InterfaceBlock) {
    val ubo = instancedInput.associatedResource as? GLSLUniformBlock ?: throw Exception("This should be an ubo!")
    bindUBO(ubo, contents)
}