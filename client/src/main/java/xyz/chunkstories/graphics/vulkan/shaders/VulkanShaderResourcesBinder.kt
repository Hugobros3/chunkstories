package xyz.chunkstories.graphics.vulkan.shaders

import xyz.chunkstories.api.graphics.rendergraph.ImageSource
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.resources.VulkanShaderResourcesContext
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture3D

/** Adapts the API-style ShaderResources provider to the Vulkan-style descriptor set instance */
fun ShaderResources.extractInto(target: VulkanShaderResourcesContext, passInstance: PassInstance) {
    parent?.extractInto(target, passInstance)

    val backend = target.pipeline.backend

    val program = target.pipeline.program.glslProgram
    val resources = program.resources
    //TODO optimize those lookups using hashmaps

    for((imageName, index, imageInput) in images) {
        resources.find { it is GLSLUniformSampledImage && it.name == imageName }?.apply {
            val sampler = target.samplers.getSamplerForImageInputParameters(imageInput)
            when(this) {
                is GLSLUniformSampledImage2D -> {
                    val texture2d = when(val source = imageInput.source) {
                        is ImageSource.AssetReference -> {
                            backend.textures.getOrLoadTexture2D(source.assetName)
                        }
                        is ImageSource.RenderBufferReference -> {
                            val vrti = passInstance.taskInstance as VulkanRenderTaskInstance
                            vrti.renderTask.buffers[source.renderBufferName]!!.texture
                        }
                        is ImageSource.TextureReference -> source.texture as VulkanTexture
                        is ImageSource.TaskOutput -> {
                            val referencedTaskInstance = source.context as VulkanRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedOutputs[source.output]!!
                            resolvedRenderBuffer.texture
                        }
                        is ImageSource.TaskOutputDepth -> {
                            val referencedTaskInstance = source.context as VulkanRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedDepthBuffer!!
                            resolvedRenderBuffer.texture
                        }
                    }
                    target.bindTextureAndSampler(imageName, texture2d, sampler, index)
                    return@apply
                }
                is GLSLUniformSampledImageCubemap -> {
                    val cubemap = when(val source = imageInput.source) {
                        is ImageSource.AssetReference -> {
                            (passInstance as VulkanPassInstance).pass.backend.textures.getOrLoadCubemap(source.assetName)
                        }
                        else -> throw Exception("Unhandled image source type ${source::class} for ${this::class}")
                    }
                    target.bindTextureAndSampler(imageName, cubemap, sampler, index)
                }
                is GLSLUniformSampledImage3D -> {
                    val texture3d = when(val source = imageInput.source) {
                        is ImageSource.TextureReference -> source.texture as VulkanTexture3D
                        else -> throw Exception("Not a 3D texture $source")
                    }
                    target.bindTextureAndSampler(imageName, texture3d, sampler, index)
                }
                else -> throw Exception("Unhandled image type :${this::class}")
            }
        }// ?: println("warning: didn't find an image named $imageName")
    }

    for((name, contents) in ubos) {
        program.resources.find { it is GLSLUniformBlock && (name == null || it.instanceName == name) && it.struct.kClass.java.isAssignableFrom(contents.javaClass) }?.apply {
            target.bindStructuredUBO(name!!, contents)
        }// ?: println("no ubo found for $name $contents")
    }
}
