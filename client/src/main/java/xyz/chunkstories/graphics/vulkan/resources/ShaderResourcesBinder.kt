package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.api.graphics.rendergraph.ImageSource
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampledImage2D
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D

fun SystemExecutionContext.bindShaderResources(target: DescriptorSetsMegapool.ShaderBindingContext) {
    shaderResources.bindTo(target, passInstance)
}

fun PassInstance.bindShaderResources(source: ShaderResources, target: DescriptorSetsMegapool.ShaderBindingContext) {
    source.bindTo(target, this)
}

/** Adapts the API-style ShaderResources provider to the Vulkan-style descriptor set instance */
private fun ShaderResources.bindTo(target: DescriptorSetsMegapool.ShaderBindingContext, passInstance: PassInstance) {
    parent?.bindTo(target, passInstance)

    val program = target.pipeline.program.glslProgram
    val resources = program.resources
    //TODO optimize those lookups using hashmaps

    for((imageName, index, imageInput) in images) {
        program.resources.find { it is GLSLUniformSampledImage && it.name == imageName }?.apply {
            val sampler = target.samplers.getSamplerForImageInputParameters(imageInput)
            when(this) {
                is GLSLUniformSampledImage2D -> {
                    val texture2d = when(val source = imageInput.source) {
                        is ImageSource.AssetReference -> {
                            (passInstance as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance).vulkanPass.backend.textures.getOrLoadTexture2D(source.assetName)
                        }
                        is ImageSource.RenderBufferReference -> {
                            val vrti = passInstance.taskInstance as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance
                            vrti.renderTask.buffers[source.renderBufferName]!!.texture
                        }
                        is ImageSource.TextureReference -> source.texture as VulkanTexture2D
                        is ImageSource.TaskOutput -> {
                            val referencedTaskInstance = source.context as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.depends.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedOutputs[source.output]!!
                            resolvedRenderBuffer.texture
                        }
                        is ImageSource.TaskOutputDepth -> {
                            val referencedTaskInstance = source.context as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance

                            //TODO we only handle direct deps for now
                            val rootPass = referencedTaskInstance.renderTask.rootPass
                            val resolvedPassInstance = referencedTaskInstance.depends.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance

                            val resolvedRenderBuffer = resolvedPassInstance.resolvedDepthBuffer!!
                            resolvedRenderBuffer.texture
                        }
                    }
                    target.bindTextureAndSampler(imageName, texture2d, sampler, index)
                    return@apply
                }
            }

            println("lol $imageName")
        }// ?: println("warning: didn't find an image named $imageName")
    }

    for((name, contents) in ubos) {
        program.resources.find { it is GLSLUniformBlock && (name == null || it.name == name) && it.struct.kClass.java.isAssignableFrom(contents.javaClass) }?.apply {
            target.bindUBO(name!!, contents)
        }// ?: println("no ubo found for $name $contents")
    }
}
