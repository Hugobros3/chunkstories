package xyz.chunkstories.graphics.vulkan.world

import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.graphics.vulkan.VulkanBackendOptions
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.vulkan.util.ShadowMappingInfo

fun doShadowMapping(ctx: SystemExecutionContext, world: World) {
    //TODO hacky
    //val client = (ctx.passInstance as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance).pass.backend.graphicsEngine.client
    val client = (world as WorldClient).client

    val mainCamera = ctx.passInstance.taskInstance.camera

    val shadowCascades = client.configuration.getIntValue(VulkanBackendOptions.shadowCascades)

    val extentPerCascadeCount = when(shadowCascades) {
        0 -> floatArrayOf()
        1 -> floatArrayOf(64f)
        2 -> floatArrayOf(128f, 32f)
        3 -> floatArrayOf(140f, 64f, 24f)
        4 -> floatArrayOf(384f, 128f, 48f, 16f)
        else -> throw Exception()
    }

    for(i in 0 until 4) {
        ctx.shaderResources.supplyImage("shadowBuffers", i, ImageInput().apply {
            source = ImageSource.AssetReference("/textures/logo.png")
            depthCompareMode = ImageInput.DepthCompareMode.SHADOWMAP
        })
    }

    val shadowInfo = ShadowMappingInfo()
    shadowInfo.cascadesCount = shadowCascades
    for(i in 0 until shadowCascades) {
        val shadowMapDepthRange = 256f
        val shadowMapExtent = extentPerCascadeCount[i]

        val shadowMapContentsMatrix = Matrix4f().ortho(-shadowMapExtent, shadowMapExtent, -shadowMapExtent, shadowMapExtent, -shadowMapDepthRange, shadowMapDepthRange, true)
        val sunPosition = world.getConditions().sunPosition.toVec3f()

        val sunLookAt = Matrix4f().lookAt(sunPosition, Vector3f(0f), Vector3f(0f, 1f, 0f))

        val shadowMatrix = Matrix4f()
        shadowMatrix.mul(shadowMapContentsMatrix, shadowMatrix)
        shadowMatrix.mul(sunLookAt, shadowMatrix)

        shadowMatrix.translate(Vector3f(mainCamera.position).negate())

        val sunCamera = Camera(viewMatrix = shadowMatrix, fov = 0f, position = mainCamera.position)

        shadowInfo.cameras[i] = sunCamera

        ctx.passInstance.dispatchRenderTask("shadowmapCascade$i", sunCamera, "sunShadow", mapOf("shadowBuffer" to RenderTarget.RenderBufferReference("shadowBuffer$i"))) {
            //val node = it as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance
            //println("RESOLVED DB: ${node.rootPassInstance.resolvedDepthBuffer}")
            //passContext.markRenderBufferAsInput(node.rootPassInstance.resolvedDepthBuffer)

            ctx.shaderResources.supplyImage("shadowBuffers", i, ImageInput().apply {
                source = renderBuffer("shadowBuffer$i")
                depthCompareMode = ImageInput.DepthCompareMode.SHADOWMAP
            })
            //ctx.shaderResources.supplyImage("shadowBuffers", ImageSource.RenderBufferReference("shadowBuffer$i"), samplerShadow, i)
        }
    }

    ctx.shaderResources.supplyUniformBlock("shadowInfo", shadowInfo)
}