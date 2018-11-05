package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration
import io.xol.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration.DepthTestMode.*
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import io.xol.chunkstories.graphics.vulkan.util.VkPipeline
import io.xol.chunkstories.graphics.vulkan.util.VkPipelineLayout
import io.xol.chunkstories.graphics.vulkan.util.VkRenderPass
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class Pipeline(val backend: VulkanGraphicsBackend, val pass: VulkanPass, val program: VulkanShaderFactory.VulkanicShaderProgram, vertexInputConfiguration: () -> VkPipelineVertexInputStateCreateInfo) {
    val layout: VkPipelineLayout
    val handle: VkPipeline

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

        val vertexInputStateCreateInfo : VkPipelineVertexInputStateCreateInfo = vertexInputConfiguration()

        // TODO get those from the VulkanPass
        val inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).apply {
            topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
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
        }

        val multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).apply {
            sampleShadingEnable(false)
            rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
        }

        val depthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO).apply {
            //depthTestEnable(pass.depth.enabled)
            depthWriteEnable(pass.depth.write)

            depthCompareOp(when(pass.depth.mode) {
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

        // TODO get those from the VulkanPass
        val staticBlendState = VkPipelineColorBlendAttachmentState.callocStack(1).apply {
            colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)

            blendEnable(true)
            srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            colorBlendOp(VK_BLEND_OP_ADD)
            srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
            dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            alphaBlendOp(VK_BLEND_OP_ADD)
        }
        val blendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).apply {
            logicOpEnable(false)
            pAttachments(staticBlendState)
        }

        val dynamicStateCreateInfo : VkPipelineDynamicStateCreateInfo? = VkPipelineDynamicStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).apply {
            pDynamicStates(stackInts(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))
        }

        val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).apply {
            val pDescriptorSets = stackMallocLong(program.descriptorSetLayouts.size)
            program.descriptorSetLayouts.forEach { pDescriptorSets.put(it) }
            pDescriptorSets.flip()

            pSetLayouts(pDescriptorSets)
            pPushConstantRanges(null)
        }

        val pPipelineLayout = stackMallocLong(1)
        vkCreatePipelineLayout(backend.logicalDevice.vkDevice, pipelineLayoutCreateInfo, null, pPipelineLayout).ensureIs("Failed to create pipeline layout", VK_SUCCESS)
        layout = pPipelineLayout.get(0)

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

            layout(layout)

            renderPass(pass.renderPass)
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
        vkDestroyPipelineLayout(backend.logicalDevice.vkDevice, layout, null)
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
