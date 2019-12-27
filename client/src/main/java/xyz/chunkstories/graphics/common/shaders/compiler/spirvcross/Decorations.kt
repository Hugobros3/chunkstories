package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import graphics.scenery.spirvcrossj.Decoration
import graphics.scenery.spirvcrossj.Resource
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler

fun ShaderCompiler.addDecorations(intermediarCompilationResults: IntermediaryCompilationResults, glslResources: List<GLSLResource>, glslInstancedInputs: List<GLSLInstancedInput>) {
    for ((stage, compiler) in intermediarCompilationResults.compilers) {
        val stageResources = compiler.shaderResources

        fun decorate(spirvResource: Resource, glslResource: GLSLResource) {
            val locator = glslResource.locator
            when (dialect) {
                GLSLDialect.VULKAN -> {
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationDescriptorSet, locator.descriptorSetSlot.toLong())
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationBinding, locator.binding.toLong())
                }
                GLSLDialect.OPENGL -> {
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationLocation, locator.binding.toLong())
                }
            }
        }

        for (i in 0 until stageResources.sampledImages.size().toInt()) {
            val spirvResource = stageResources.sampledImages[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLResource

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.separateImages.size().toInt()) {
            val spirvResource = stageResources.separateImages[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformImage2D

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.separateSamplers.size().toInt()) {
            val spirvResource = stageResources.separateSamplers[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformSampler

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.uniformBuffers.size().toInt()) {
            val spirvResource = stageResources.uniformBuffers[i]
            //val instanceName = spirvResource.name.split("_")[2]

            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformBlock

            decorate(spirvResource, glslResource)
        }

        //TODO SSBOS
        for (i in 0 until stageResources.storageBuffers.size().toInt()) {
            val spirvResource = stageResources.storageBuffers[i]
            //val instanceName = spirvResource.name.split("_")[2]

            //val glslInstancedInput = glslInstancedInputs.find { it.name == instanceName }!!
            //val glslResource = glslInstancedInput.associatedResource as GLSLShaderStorage
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLShaderStorage

            decorate(spirvResource, glslResource)
        }
    }
}
