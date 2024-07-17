package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import de.unisaarland.zhady.*
import org.lwjgl.system.MemoryStack
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.compiler.zhady.IntermediaryCompilationResults

fun ShaderCompiler.addDecorations(intermediarCompilationResults: IntermediaryCompilationResults, glslResources: List<GLSLResource>, glslInstancedInputs: List<GLSLInstancedInput>) {
    for ((stage, module) in intermediarCompilationResults.tShaders) {
        //val stageResources = compiler.aashaderResources

        for (res in glslResources) {
            val decl = shady.get_declaration(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false), res.name)
            if (Node_.getCPtr(decl) == 0L)
                continue

            var annotations = shady.get_declaration_annotations(decl)

            when (dialect) {
                GLSLDialect.VULKAN -> {

                    annotations = shady.append_nodes(decl.arena, annotations, shady.annotation_value(decl.arena, AnnotationValue().apply {
                        name = "DescriptorSet"
                        value = shady.int32_literal(decl.arena, res.locator.descriptorSetSlot)
                    }))
                    annotations = shady.append_nodes(decl.arena, annotations, shady.annotation_value(decl.arena, AnnotationValue().apply {
                        name = "DescriptorBinding"
                        value = shady.int32_literal(decl.arena, res.locator.binding)
                    }))
                }
                GLSLDialect.OPENGL -> {
                    TODO()
                    //compiler.setDecoration(spirvResource.id(), Decoration.DecorationLocation, locator.binding.toLong())
                }
            }
        }

        shady.dump_module(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        /*
        fun decorate(spirvResource: SpvcReflectedResource, glslResource: GLSLResource) {
            val locator = glslResource.locator
            when (dialect) {
                GLSLDialect.VULKAN -> {
                    compiler.setDecoration(spirvResource.id(), Decoration.DecorationDescriptorSet, locator.descriptorSetSlot.toLong())
                    compiler.setDecoration(spirvResource.id(), Decoration.DecorationBinding, locator.binding.toLong())
                }
                GLSLDialect.OPENGL -> {
                    compiler.setDecoration(spirvResource.id(), Decoration.DecorationLocation, locator.binding.toLong())
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
        }*/
    }
}
