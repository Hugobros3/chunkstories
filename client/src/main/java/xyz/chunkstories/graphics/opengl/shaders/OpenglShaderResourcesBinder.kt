package xyz.chunkstories.graphics.opengl.shaders

import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2D
import xyz.chunkstories.graphics.opengl.FakePSO
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*

fun FakePSO.bindTexture(textureName: String, texture: OpenglTexture2D) {
    val resource = program.glslProgram.resources.find {
        it is GLSLUniformSampledImage2D && it.name == textureName
    } as? GLSLUniformSampledImage2D ?: return

    glBindTextureUnit(resource.openglTextureUnit, texture.glTexId)

    val uniformLocation = glGetUniformLocation(program.programId, resource.name)
    //println("$uniformLocation vs ${resource.binding}")
    glUniform1i(uniformLocation, resource.openglTextureUnit)
}