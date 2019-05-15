package xyz.chunkstories.graphics.opengl.world.chunks

import org.lwjgl.opengl.GL30.GL_TRIANGLES
import org.lwjgl.opengl.GL30.glDrawArrays
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL32.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.common.world.ChunkRenderInfo
import xyz.chunkstories.graphics.opengl.*
import xyz.chunkstories.graphics.opengl.buffers.OpenglUniformBuffer
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderProgram
import xyz.chunkstories.graphics.opengl.shaders.bindShaderResources
import xyz.chunkstories.graphics.opengl.shaders.bindTexture
import xyz.chunkstories.graphics.opengl.shaders.bindUBO
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.graphics.opengl.voxels.OpenglVoxelTexturesArray

class OpenglChunkRepresentationsDispatcher(backend: OpenglGraphicsBackend) : OpenglDispatchingSystem<OpenglChunkRepresentation>(backend) {

    override val representationName: String = OpenglChunkRepresentation::class.java.canonicalName

    val vertexInputConfiguration = vertexInputConfiguration {
        var o = 0

        attribute {
            binding = 0
            locationName = "vertexIn"
            format = Pair(VertexFormat.FLOAT, 3)
            offset = o
        }
        o += 4 * 3

        attribute {
            binding = 0
            locationName = "colorIn"
            format = Pair(VertexFormat.NORMALIZED_UBYTE, 4)
            offset = o
        }
        o += 4

        attribute {
            binding = 0
            locationName = "normalIn"
            format = Pair(VertexFormat.NORMALIZED_BYTE, 4)
            offset = o
        }
        o += 4

        attribute {
            binding = 0
            locationName = "texCoordIn"
            format = Pair(VertexFormat.NORMALIZED_USHORT, 2)
            //format(VK_FORMAT_R16G16_UNORM)
            offset = o
        }
        o += 4

        attribute {
            binding = 0
            locationName = "textureIdIn"
            format = Pair(VertexFormat.UINT, 1)
            //format(VK_FORMAT_R32_UINT)
            offset = o
        }
        o += 4
        o += 4

        binding {
            binding = 0
            stride = o
            inputRate = InputRate.PER_VERTEX
        }
    }

    inner class Drawer(pass: OpenglPass, initCode: Drawer.() -> Unit) : OpenglDispatchingSystem.Drawer<OpenglChunkRepresentation.Section>(pass), ChunksRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: OpenglDispatchingSystem<*>
            get() = this@OpenglChunkRepresentationsDispatcher

        init {
            apply(initCode)
        }

        val program = backend.shaderFactory.createProgram(shader, ShaderCompilationParameters(outputs = pass.declaration.outputs))
        val pipeline = FakePSO(backend, program, pass, vertexInputConfiguration, FaceCullingMode.CULL_BACK)

        //TODO figure out instanced inputs in GL 3.3
        val chunkInfoII = program.glslProgram.instancedInputs.find { it.name == "chunkInfo" }!!
        val chunkInfoStruct = chunkInfoII.struct
        val structSize = chunkInfoStruct.size
        val sizeAligned16 = getStd140AlignedSizeForStruct(chunkInfoStruct)

        val maxChunksRendered = 2048
        val ssboBufferSize = (sizeAligned16 * maxChunksRendered)

        override fun executeDrawingCommands(frame: OpenglFrame, context: SystemExecutionContext, work: Sequence<OpenglChunkRepresentation.Section>) {
            val client = backend.window.client.ingame ?: return

            val staticMeshes = work.mapNotNull { it.staticMesh }

            pipeline.bind()

            val camera = context.passInstance.taskInstance.camera
            val world = client.world

            pipeline.bindUBO("camera", camera)
            pipeline.bindUBO("world", world.getConditions())

            // prepare uniform buffer
            /*val chunkInfoUBO = OpenglUniformBuffer(backend, chunkInfoStruct)
            val chunkInfoBB = memAlloc(ssboBufferSize)
            for((i, item) in work.withIndex()) {
                val chunkRepresentation = item.parent
                val chunkRenderInfo = ChunkRenderInfo().apply {
                    chunkX = chunkRepresentation.chunk.chunkX
                    chunkY = chunkRepresentation.chunk.chunkY
                    chunkZ = chunkRepresentation.chunk.chunkZ
                }
                extractInterfaceBlock(chunkInfoBB, i * sizeAligned16, chunkRenderInfo, chunkInfoStruct)
            }
            chunkInfoBB.flip()
            chunkInfoUBO.upload(chunkInfoBB)

            memFree(chunkInfoBB)*/

            val chunkInfoUboResource = chunkInfoII.associatedResource as GLSLUniformBlock

            val voxelTexturesArray = client.content.voxels().textures() as OpenglVoxelTexturesArray
            pipeline.bindTexture("albedoTextures", voxelTexturesArray.albedoOnionTexture)
            //glBindBufferRange(GL_UNIFORM_BUFFER, program.uboBindings[chunkInfoUboResource]!!, chunkInfoUBO.glId, 0L, ssboBufferSize.toLong())

            for(item in work) {
                val staticMesh = item.staticMesh ?: continue

                val chunkRepresentation = item.parent
                val chunkRenderInfo = ChunkRenderInfo().apply {
                    chunkX = chunkRepresentation.chunk.chunkX
                    chunkY = chunkRepresentation.chunk.chunkY
                    chunkZ = chunkRepresentation.chunk.chunkZ
                }
                pipeline.bindUBO(chunkInfoUboResource.rawName, chunkRenderInfo)
                pipeline.bindVertexBuffer(0, staticMesh.buffer)
                glDrawArrays(GL_TRIANGLES, 0, staticMesh.count)
                //println("wow"+staticMesh.count)
            }

            pipeline.unbind()

            //TODO cheeky :)
            //chunkInfoUBO.cleanup()
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()
        }
    }

    override fun createDrawerForPass(pass: OpenglPass, drawerInitCode: OpenglDispatchingSystem.Drawer<*>.() -> Unit) = Drawer(pass, drawerInitCode)

    /*init {
        dslCode()

        program = backend.shaderFactory.createProgram(shader)
        pipeline = FakePSO(backend, program, pass, vertexInputConfiguration, FaceCullingMode.CULL_BACK)

        val vertices = floatArrayOf(-1.0F, -3.0F, 3.0F, 1.0F, -1.0F, 1.0F)
        vertexBuffer = OpenglVertexBuffer(backend)

        MemoryStack.stackPush().use {
            val byteBuffer = MemoryStack.stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun executeDrawingCommands(frame: OpenglFrame, ctx: SystemExecutionContext) {
        pipeline.bind()
        pipeline.bindVertexBuffer(0, vertexBuffer)
        ctx.bindShaderResources(pipeline)
        glDrawArrays(GL_TRIANGLES, 0, 3)
        pipeline.unbind()
    }*/

    override fun sort(representation: OpenglChunkRepresentation, drawers: Array<OpenglDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {
        //TODO look at material/tag and decide where to send it
        for (section in representation.sections.values) {
            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as Drawer).materialTag == section.materialTag) {
                    outputs[index].add(section)
                }
            }
        }
    }

    override fun cleanup() {
    }
}