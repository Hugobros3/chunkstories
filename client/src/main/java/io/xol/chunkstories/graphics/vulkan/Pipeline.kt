package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.graphics.vulkan.shaderc.VulkanShaderFactory
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class Pipeline(val backend: VulkanGraphicsBackend, val renderPass: VulkanRenderPass, program: VulkanShaderFactory.VulkanicShaderProgram) {
    val layout: VkPipelineLayout
    val handle: VkPipeline

    init {
        logger.info("Creating pipeline")

        stackPush()

        val vertexStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).apply {
            stage(VK_SHADER_STAGE_VERTEX_BIT)
            pName(stackUTF8("main"))
            //TODO module(vertexShaderModule.handle)
        }

        val fragmentStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).apply {
            stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            pName(stackUTF8("main"))
            //TODO module(fragmentShaderModule.handle)
        }

        val shaderStagesCreateInfo = VkPipelineShaderStageCreateInfo.callocStack(2)
        shaderStagesCreateInfo.put(vertexStageCreateInfo)
        shaderStagesCreateInfo.put(fragmentStageCreateInfo)
        shaderStagesCreateInfo.flip()

        // Vertex input
        val bindingDescription = VkVertexInputBindingDescription.callocStack(1).apply {
            binding(0)
            stride(2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        val attributeDescriptions = VkVertexInputAttributeDescription.callocStack(1).apply {
            binding(0)
            location(0)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }

        val vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO).apply {
            pVertexBindingDescriptions(bindingDescription)
            pVertexAttributeDescriptions(attributeDescriptions)
        }

        val inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).apply {
            topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            primitiveRestartEnable(false)
        }

        //TODO use dynamic state for this
        val viewport = VkViewport.callocStack(1).apply {
            x(0.0F)
            y(0.0F)
            width(backend.window.width.toFloat())
            height(backend.window.height.toFloat())
            minDepth(0.0F)
            maxDepth(1.0F)
        }

        val zeroZero = VkOffset2D.callocStack().apply {
            x(0)
            y(0)
        }
        val scissor = VkRect2D.callocStack(1).apply {
            offset(zeroZero)
            extent().width(backend.window.width)
            extent().height(backend.window.height)
            //extent(backend.physicalDevice.swapchainDetails.swapExtentToUse)
        }

        val viewportStageCreateInfo = VkPipelineViewportStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).apply {
            viewportCount(1)
            pViewports(viewport)
            scissorCount(1)
            pScissors(scissor)
        }

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

        val staticBlendState = VkPipelineColorBlendAttachmentState.callocStack(1).apply {
            colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            blendEnable(false)
        }

        val blendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).apply {
            logicOpEnable(false)
            pAttachments(staticBlendState)
        }

        //TODO here goes VkPipelineDynamicStateCreateInfo
        val dynamicStateCreateInfo : VkPipelineDynamicStateCreateInfo? = null

        val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).apply {
            pSetLayouts(null)
            pPushConstantRanges(null)
        }

        val pPipelineLayout = stackMallocLong(1)
        vkCreatePipelineLayout(backend.logicalDevice.vkDevice, pipelineLayoutCreateInfo, null, pPipelineLayout).ensureIs("Failed to create pipeline layout", VK_SUCCESS)
        layout = pPipelineLayout.get(0)

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.callocStack(1).sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).apply {
            pStages(shaderStagesCreateInfo)
            pVertexInputState(vertexInputInfo)
            pInputAssemblyState(inputAssemblyStateCreateInfo)
            pViewportState(viewportStageCreateInfo)
            pRasterizationState(rasterizerCreateInfo)
            pMultisampleState(multisampleStateCreateInfo)
            pDepthStencilState(null)
            pColorBlendState(blendStateCreateInfo)
            pDynamicState(null)//TODO

            layout(layout)

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
        vkDestroyPipelineLayout(backend.logicalDevice.vkDevice, layout, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}