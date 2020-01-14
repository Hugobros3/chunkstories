package xyz.chunkstories.graphics.opengl.shaders

import xyz.chunkstories.api.graphics.rendergraph.ImageSource
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2D
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImageCubemap
import xyz.chunkstories.graphics.opengl.FakePSO
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance
import xyz.chunkstories.graphics.opengl.graph.OpenglRenderTaskInstance
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D

fun OpenglPassInstance.bindShaderResources(pipeline: FakePSO) = this.shaderResources.extractIn(pipeline, this)

fun ShaderResources.extractIn(target: FakePSO, passInstance: PassInstance) {
    parent?.extractIn(target, passInstance)

    val backend = target.backend

    val program = target.program.glslProgram
    val resources = program.resources
    //TODO optimize those lookups using hashmaps

    for ((imageName, index, imageInput) in images) {
        resources.find { it is GLSLUniformSampledImage && it.name == imageName }?.apply {
            //TODO
            val sampler = target.backend.samplers.getSamplerForImageInputParameters(imageInput)

            when (this) {
                is GLSLUniformSampledImage2D -> {
                    val texture2d = when (val source = imageInput.source) {
                        is ImageSource.AssetReference -> {
                            backend.textures.getOrLoadTexture2D(source.assetName)
                        }
                        is ImageSource.RenderBufferReference -> {
                            val vrti = passInstance.taskInstance as OpenglRenderTaskInstance
                            vrti.renderTask.buffers[source.renderBufferName]!!.texture
                        }
                        is ImageSource.TextureReference -> source.texture as OpenglTexture2D
                        is ImageSource.TaskOutput -> {
                            val referencedTaskInstance = source.context as OpenglRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as OpenglPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedColorOutputs[source.output]!!
                            resolvedRenderBuffer.texture
                        }
                        is ImageSource.TaskOutputDepth -> {
                            val referencedTaskInstance = source.context as OpenglRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as OpenglPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedDepth!!
                            resolvedRenderBuffer.texture
                        }
                        else -> TODO()
                    }

                    //TODO
                    //target.bindTextureAndSampler(imageName, texture2d, sampler, index)µ
                    target.bindTexture(imageName, index, texture2d, sampler)
                    return@apply
                }
                is GLSLUniformSampledImageCubemap -> {
                    val cubemap = when (val source = imageInput.source) {
                        is ImageSource.AssetReference -> {
                            (passInstance as OpenglPassInstance).pass.backend.textures.getOrLoadCubemap(source.assetName)
                        }
                        else -> throw Exception("Unhandled image source type ${source::class} for ${this::class}")
                    }

                    //TODO
                    //target.bindTextureAndSampler(imageName, cubemap, sampler, index)
                    target.bindTexture(imageName, cubemap)
                }
                else -> throw Exception("Unhandled image type :${this::class}")
            }
        }// ?: println("warning: didn't find an image named $imageName")
    }

    for ((name, contents) in ubos) {
        program.resources.find { it is GLSLUniformBlock && (name == null || it.instanceName == name) && it.struct.kClass.java.isAssignableFrom(contents.javaClass) }?.apply {
            target.bindStructuredUBO(name!!, contents)
        }// ?: println("no ubo found for $name $contents")
    }
}
