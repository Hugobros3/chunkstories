package xyz.chunkstories.graphics.vulkan

import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration.DepthTestMode.*
import xyz.chunkstories.api.graphics.rendergraph.PassOutput
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.util.VkPipeline
import xyz.chunkstories.graphics.vulkan.util.VkPipelineLayout
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration
import xyz.chunkstories.api.graphics.rendergraph.PassOutputsDeclaration
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram

fun vertexInputConfiguration(declaration: VertexInputConfigurationContext.() -> Unit) = VertexInputConfiguration(declaration)

data class VertexInputConfiguration(val declaration: VertexInputConfigurationContext.() -> Unit)

interface VertexInputConfigurationContext {
    val program: GLSLProgram

    fun binding(decl: VkVertexInputBindingDescription.() -> Unit)

    fun attribute(decl: VkVertexInputAttributeDescription.() -> Unit)
}

class Pipeline(val backend: VulkanGraphicsBackend, val program : VulkanShaderProgram, val renderPass: RenderPass, val outputs: PassOutputsDeclaration, val depth: DepthTestingConfiguration, val vertexInputConfiguration: VertexInputConfiguration, val primitiveType: Primitive, val faceCullingMode: FaceCullingMode) {
    val pipelineLayout: VkPipelineLayout
    val handle: VkPipeline

    constructor(backend: VulkanGraphicsBackend, program: VulkanShaderProgram, pass: VulkanPass, vertexInputConfiguration: VertexInputConfiguration, primitiveType: Primitive, faceCullingMode: FaceCullingMode) :
            this(backend, program, pass.renderpass, pass.declaration.outputs, pass.declaration.depthTestingConfiguration, vertexInputConfiguration, primitiveType, faceCullingMode)

    init {
        logger.info("Creating pipeline")

        stackPush()
        val vertexStagesCreateInfos = program.modules.map { (stage, module) ->
            VkPipelineShaderStageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).apply {
                stage(stage.vkShaderStageBit)
                pName(stackUTF8("main"))
                module(module.handle)
            }
        }

        val shaderStagesCreateInfo = VkPipelineShaderStageCreateInfo.callocStack(vertexStagesCreateInfos.size)
        vertexStagesCreateInfos.forEach { shaderStagesCreateInfo.put(it) }
        shaderStagesCreateInfo.flip()

        fun VertexInputConfiguration.fillVkVertexInputStruct(struct: VkPipelineVertexInputStateCreateInfo) {
            val bindings = mutableListOf<VkVertexInputBindingDescription>()
            val attributes = mutableListOf<VkVertexInputAttributeDescription>()

            val localConfigCtx = object : VertexInputConfigurationContext {
                override val program: GLSLProgram
                    get() = this@Pipeline.program.glslProgram

                override fun binding(decl: VkVertexInputBindingDescription.() -> Unit) {
                    bindings += VkVertexInputBindingDescription.callocStack().apply(decl)
                }

                override fun attribute(decl: VkVertexInputAttributeDescription.() -> Unit) {
                    attributes += VkVertexInputAttributeDescription.callocStack().apply(decl)
                }
            }

            this.declaration(localConfigCtx)

            struct.apply {
                val vkBindings = VkVertexInputBindingDescription.callocStack(bindings.size)
                bindings.forEachIndexed { i, d -> vkBindings[i].set(d) }
                pVertexBindingDescriptions(vkBindings)

                val vkAttributes = VkVertexInputAttributeDescription.callocStack(attributes.size)
                attributes.forEachIndexed { i, d -> vkAttributes[i].set(d) }
                pVertexAttributeDescriptions(vkAttributes)
            }
        }

        val vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
        vertexInputConfiguration.fillVkVertexInputStruct(vertexInputStateCreateInfo)

        // TODO get those from the VulkanPass
        val inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).apply {
            topology(when(primitiveType) {
                Primitive.POINTS -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST
                Primitive.LINES -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST
                Primitive.TRIANGLES -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST
            })
            primitiveRestartEnable(false)
        }

        // Specify a count but ignore filling the structs, we'll do it in the command buffer !
        val viewportStageCreateInfo = VkPipelineViewportStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).apply {
            viewportCount(1)
            scissorCount(1)
        }

        // TODO get those from the VulkanPass
        val rasterizerCreateInfo = VkPipelineRasterizationStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO).apply {
            depthClampEnable(false)
            rasterizerDiscardEnable(false)
            polygonMode(VK_POLYGON_MODE_FILL)
            lineWidth(1.0F)
            depthBiasEnable(false)
            cullMode(when(faceCullingMode){
                FaceCullingMode.DISABLED -> VK_CULL_MODE_NONE
                FaceCullingMode.CULL_FRONT -> VK_CULL_MODE_FRONT_BIT
                FaceCullingMode.CULL_BACK -> VK_CULL_MODE_BACK_BIT
            })
            frontFace(VK_FRONT_FACE_CLOCKWISE)
        }

        val multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).apply {
            sampleShadingEnable(false)
            rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
        }

        val depthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO).apply {
            depthTestEnable(depth.enabled)
            depthWriteEnable(depth.write)

            depthCompareOp(when(depth.mode) {
                GREATER -> VK_COMPARE_OP_GREATER
                GREATER_OR_EQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL
                EQUAL -> VK_COMPARE_OP_EQUAL
                LESS_OR_EQUAL -> VK_COMPARE_OP_LESS_OR_EQUAL
                LESS -> VK_COMPARE_OP_LESS
                ALWAYS -> VK_COMPARE_OP_ALWAYS
            })

            depthBoundsTestEnable(false)
            minDepthBounds(0f)
            maxDepthBounds(1f)

            stencilTestEnable(false)
        }

        val staticBlendState = VkPipelineColorBlendAttachmentState.callocStack(outputs.outputs.size)
        outputs.outputs.forEachIndexed { index, passOutput -> staticBlendState[index].apply {
            colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)

            when(passOutput.blending) {
                PassOutput.BlendMode.OVERWRITE -> blendEnable(false)
                PassOutput.BlendMode.ALPHA_TEST -> {
                    // Same as MIX here
                    blendEnable(true)
                    srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                    dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    colorBlendOp(VK_BLEND_OP_ADD)
                    srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    alphaBlendOp(VK_BLEND_OP_ADD)
                }
                PassOutput.BlendMode.MIX -> {
                    blendEnable(true)
                    srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                    dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    colorBlendOp(VK_BLEND_OP_ADD)
                    srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    alphaBlendOp(VK_BLEND_OP_ADD)
                }
                PassOutput.BlendMode.ADD -> TODO()
                PassOutput.BlendMode.PREMULTIPLIED_ALPHA -> TODO()
            }
        } }

        val blendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).apply {
            logicOpEnable(false)
            pAttachments(staticBlendState)
        }

        val dynamicStateCreateInfo : VkPipelineDynamicStateCreateInfo? = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).apply {
            pDynamicStates(stackInts(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))
        }

        val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).apply {
            val pDescriptorSets = stackMallocLong(program.slotLayouts.size)
            program.slotLayouts.forEach { pDescriptorSets.put(it.vulkanLayout) }
            pDescriptorSets.flip()

            pSetLayouts(pDescriptorSets)
            pPushConstantRanges(null)
        }

        val pPipelineLayout = stackMallocLong(1)
        vkCreatePipelineLayout(backend.logicalDevice.vkDevice, pipelineLayoutCreateInfo, null, pPipelineLayout).ensureIs("Failed to create pipeline layout", VK_SUCCESS)
        pipelineLayout = pPipelineLayout.get(0)

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.callocStack(1).sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).apply {
            pStages(shaderStagesCreateInfo)
            pVertexInputState(vertexInputStateCreateInfo)
            pInputAssemblyState(inputAssemblyStateCreateInfo)

            pDynamicState(dynamicStateCreateInfo)

            pViewportState(viewportStageCreateInfo)
            pRasterizationState(rasterizerCreateInfo)
            pMultisampleState(multisampleStateCreateInfo)
            pDepthStencilState(depthStencilStateCreateInfo)
            pColorBlendState(blendStateCreateInfo)

            layout(pipelineLayout)

            //TODO handle subpasses ?
            renderPass(renderPass.handle)
            subpass(0)

            basePipelineHandle(VK_NULL_HANDLE)
            basePipelineIndex(-1) // this references another object in the struct of pipelineCreateInfo
        }

        val pPipeline = stackMallocLong(1)
        vkCreateGraphicsPipelines(backend.logicalDevice.vkDevice, /** no pipeline cache */ VK_NULL_HANDLE,  pipelineCreateInfo, null, pPipeline).ensureIs("Failed to create graphics pipeline", VK_SUCCESS)
        handle = pPipeline.get(0)

        stackPop()
    }

    fun cleanup() {
        vkDestroyPipeline(backend.logicalDevice.vkDevice, handle, null)
        vkDestroyPipelineLayout(backend.logicalDevice.vkDevice, pipelineLayout, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}

val ShaderStage.vkShaderStageBit: Int
    get() = when(this) {

        ShaderStage.VERTEX -> VK_SHADER_STAGE_VERTEX_BIT
        ShaderStage.GEOMETRY -> VK_SHADER_STAGE_GEOMETRY_BIT
        ShaderStage.FRAGMENT -> VK_SHADER_STAGE_FRAGMENT_BIT
    }
