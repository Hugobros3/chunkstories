package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import io.xol.chunkstories.graphics.vulkan.util.VkPipeline
import io.xol.chunkstories.graphics.vulkan.util.VkPipelineLayout
import io.xol.chunkstories.graphics.vulkan.util.VkRenderPass
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class Pipeline(val backend: VulkanGraphicsBackend, val renderPass: VkRenderPass, val program: VulkanShaderFactory.VulkanicShaderProgram) {
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

        // Vertex input
        // TODO obtain those from the shader program ?
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

        // Specify a count but ignore filling the structs, we'll do it in the command buffer !
        val viewportStageCreateInfo = VkPipelineViewportStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).apply {
            viewportCount(1)
            scissorCount(1)
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
            pVertexInputState(vertexInputInfo)
            pInputAssemblyState(inputAssemblyStateCreateInfo)

            pDynamicState(dynamicStateCreateInfo)

            pViewportState(viewportStageCreateInfo)
            pRasterizationState(rasterizerCreateInfo)
            pMultisampleState(multisampleStateCreateInfo)
            pDepthStencilState(null)
            pColorBlendState(blendStateCreateInfo)

            layout(layout)

            renderPass(renderPass)
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
