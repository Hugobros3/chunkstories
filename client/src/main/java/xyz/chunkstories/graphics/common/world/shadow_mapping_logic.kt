package xyz.chunkstories.graphics.common.world

import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.World
import xyz.chunkstories.graphics.common.CommonGraphicsOptions
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.structs.ShadowMappingInfo
import xyz.chunkstories.world.WorldImplementation

fun doShadowMapping(ctx: PassInstance, world: World) {
    val client = (world as WorldImplementation).gameInstance.engine as Client

    val mainCamera = ctx.taskInstance.camera

    val shadowCascades = client.configuration.getIntValue(CommonGraphicsOptions.shadowCascades)

    val extentPerCascadeCount = when(shadowCascades) {
        0 -> floatArrayOf()
        1 -> floatArrayOf(64f)
        2 -> floatArrayOf(128f, 32f)
        3 -> floatArrayOf(384f, 96f, 24f)
        4 -> floatArrayOf(768f, 256f, 64f, 16f)
        else -> throw Exception()
    }

    for(i in 0 until 4) {
        ctx.shaderResources.supplyImage("shadowBuffers", i, ImageInput().apply {
            source = ImageSource.AssetReference("/textures/logo.png")
            depthCompareMode = ImageInput.DepthCompareMode.GREATER_OR_EQUAL
        })
    }

    val shadowInfo = ShadowMappingInfo()
    shadowInfo.cascadesCount = shadowCascades
    for(i in 0 until shadowCascades) {
        val shadowMapDepthRange = 256f
        val shadowMapExtent = extentPerCascadeCount[i]

        val shadowMapContentsMatrix = Matrix4f().ortho(-shadowMapExtent, shadowMapExtent, -shadowMapExtent, shadowMapExtent, shadowMapDepthRange, -shadowMapDepthRange, true)
        val sunPosition = world.getConditions().sunPosition.toVec3f()

        val sunLookAt = Matrix4f().lookAt(sunPosition, Vector3f(0f), Vector3f(0f, 1f, 0f))

        val shadowMatrix = Matrix4f()
        shadowMatrix.mul(shadowMapContentsMatrix, shadowMatrix)
        shadowMatrix.mul(sunLookAt, shadowMatrix)

        shadowMatrix.translate(Vector3f(mainCamera.position.toVec3f()).negate())

        val sunCamera = Camera(viewMatrix = shadowMatrix, fov = 0f, position = mainCamera.position)

        shadowInfo.cameras[i] = sunCamera

        ctx.dispatchRenderTask("shadowmapCascade$i", sunCamera, "sunShadow", mapOf("shadowBuffer" to RenderTarget.RenderBufferReference("shadowBuffer$i"))) {
            //val node = it as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance
            //println("RESOLVED DB: ${node.rootPassInstance.resolvedDepthBuffer}")
            //passContext.markRenderBufferAsInput(node.rootPassInstance.resolvedDepthBuffer)

            ctx.shaderResources.supplyImage("shadowBuffers", i, ImageInput().apply {
                source = renderBuffer("shadowBuffer$i")
                scalingMode = ImageInput.ScalingMode.LINEAR
                depthCompareMode = ImageInput.DepthCompareMode.GREATER_OR_EQUAL
            })
            //ctx.shaderResources.supplyImage("shadowBuffers", ImageSource.RenderBufferReference("shadowBuffer$i"), samplerShadow, i)
        }
    }

    ctx.shaderResources.supplyUniformBlock("shadowInfo", shadowInfo)
}