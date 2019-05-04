package xyz.chunkstories.graphics.vulkan.systems.world

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.util.kotlin.getNormalMatrix
import xyz.chunkstories.api.voxel.ChunkMeshRenderingInterface
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.UnitCube
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.textures.voxels.VoxelTexturesArray
import xyz.chunkstories.world.cell.ScratchCell
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.util.*

class TaskCreateChunkMesh(val backend: VulkanGraphicsBackend, val chunk: CubicChunk, attachedProperty: AutoRebuildingProperty, updates: Int) : AutoRebuildingProperty.UpdateTask(attachedProperty, updates) {
    lateinit var rawChunkData: IntArray

    inline fun opaque(voxel: Voxel) = voxel.opaque// || voxel.name == "water"

    inline fun opaque(data: Int) = if (data == 0) false else opaque(chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data))!!)

    inline fun opaque(x2: Int, y2: Int, z2: Int): Boolean = opaque(data(x2, y2, z2))

    inline fun data(x2: Int, y2: Int, z2: Int): Int =
            if (x2 in 0..31 && y2 in 0..31 && z2 in 0..31)
                rawChunkData[x2 * 32 * 32 + y2 * 32 + z2]
            else
                chunk.world.peekRaw(x2 + chunk.chunkX * 32, y2 + chunk.chunkY * 32, z2 + chunk.chunkZ * 32)

    override fun update(taskExecutor: TaskExecutor): Boolean {
        if (chunk.holder().state !is ChunkHolder.State.Available)
            return true

        val neighborsPresent = neighborsIndexes.count { (x, y, z) ->
            val neighbor = chunk.world.getChunk(chunk.chunkX + x, chunk.chunkY + y, chunk.chunkZ + z)
            (neighbor != null || (chunk.chunkY + y < 0) || (chunk.chunkY + y >= chunk.world.worldInfo.size.heightInChunks))
        }
        if (neighborsPresent < neighborsIndexes.size)
            return true

        val receiver = chunk.mesh as VulkanChunkMeshProperty

        val rng = Random(1)

        val chunkDataRef = chunk.voxelDataArray

        class ScratchBuffer : Cleanable {
            val cubesData: ByteBuffer = MemoryUtil.memAlloc(1024 * 1024 * 4 * 4)
            var cubesCount = 0
            val meshData: ByteBuffer = MemoryUtil.memAlloc(1024 * 1024 * 4 * 4)
            var meshTriCount = 0
            override fun cleanup() {
                memFree(cubesData)
                memFree(meshData)
            }
        }

        val map = mutableMapOf<String, ScratchBuffer>()

        if (chunk.isAirChunk || chunkDataRef == null) {

        } else {
            rawChunkData = chunkDataRef

            val cell = ScratchCell(chunk.world)
            var cellData = 0

            val mesher = object : ChunkMeshRenderingInterface {
                var x = 0; var y = 0; var z = 0

                override fun addModel(model: Model, matrix: Matrix4f?, materialsOverrides: Map<Int, MeshMaterial>) {
                    val sunlight = VoxelFormat.sunlight(cellData)
                    val blocklight = VoxelFormat.blocklight(cellData)

                    var ox = x //+ (offset?.x() ?: 0f)
                    var oy = y //+ (offset?.y() ?: 0f)
                    var oz = z //+ (offset?.z() ?: 0f)

                    val normalMatrix = matrix?.getNormalMatrix()
                    val vertex = Vector4f()
                    val normal = Vector3f()

                    for ((index, mesh) in model.meshes.withIndex()) {
                        val material = materialsOverrides[index] ?: mesh.material
                        val scratch = map.getOrPut(material.tag) { ScratchBuffer() }
                        val meshData = scratch.meshData
                        var texName = material.textures["albedoTexture"] ?: "notex"
                        val asset = chunk.world.content.getAsset(texName)
                        val assetName = asset?.name
                        when {
                            assetName != null && assetName.startsWith("voxels/blockmodels") -> texName = assetName.removePrefix("voxels/blockmodels/") ?: "notex"
                            assetName != null && assetName.startsWith("voxels/textures") -> texName = assetName.removePrefix("voxels/textures/") ?: "notex"
                        }
                        texName = texName.removeSuffix(".png")
                        val voxelTexture = chunk.world.content.voxels().textures().get(texName) as VoxelTexturesArray.VoxelTextureInArray
                        val textureId = voxelTexture.textureArrayIndex
                        val vertexIn = mesh.attributes.find { it.name == "vertexIn" }?.data!!
                        val normalIn = mesh.attributes.find { it.name == "normalIn" }?.data
                        val texCoordIn = mesh.attributes.find { it.name == "texCoordIn" }?.data
                        for (i in 0 until mesh.vertices) {
                            if(matrix != null) {
                                vertex.set(vertexIn.getFloat(i * 12 + 0), vertexIn.getFloat(i * 12 + 4), vertexIn.getFloat(i * 12 + 8), 1f)
                                matrix.transform(vertex)

                                meshData.putFloat(vertex.x + ox)
                                meshData.putFloat(vertex.y + oy)
                                meshData.putFloat(vertex.z + oz)
                            } else {
                                meshData.putFloat(vertexIn.getFloat(i * 12 + 0) + ox)
                                meshData.putFloat(vertexIn.getFloat(i * 12 + 4) + oy)
                                meshData.putFloat(vertexIn.getFloat(i * 12 + 8) + oz)
                            }

                            meshData.put((sunlight * 16).toByte())
                            meshData.put((blocklight * 16).toByte())
                            meshData.put(0)
                            meshData.put(0)

                            if (normalIn != null) {
                                if(normalMatrix != null) {
                                    normal.set(normalIn.getFloat(i * 12 + 0), normalIn.getFloat(i * 12 + 4 ), normalIn.getFloat(i * 12 + 8))
                                    normalMatrix.transform(normal)

                                    meshData.put((normal.x).toSNORM())
                                    meshData.put((normal.y).toSNORM())
                                    meshData.put((normal.z).toSNORM())
                                    meshData.put(0)
                                } else {
                                    meshData.put(normalIn.getFloat(i * 12 + 0).toSNORM())
                                    meshData.put(normalIn.getFloat(i * 12 + 4).toSNORM())
                                    meshData.put(normalIn.getFloat(i * 12 + 8).toSNORM())
                                    meshData.put(0)
                                }
                            } else {
                                meshData.put(0)
                                meshData.put(0)
                                meshData.put(0)
                                meshData.put(0)
                            }

                            if (texCoordIn != null) {
                                meshData.putShort(texCoordIn.getFloat(i * 8 + 0).toUNORM16())
                                meshData.putShort(texCoordIn.getFloat(i * 8 + 4).toUNORM16())
                            } else {
                                meshData.put(0)
                                meshData.put(0)
                                meshData.put(0)
                                meshData.put(0)
                            }

                            meshData.putInt(textureId)
                            scratch.meshTriCount++
                        }
                    }
                }
            }

            for (x in 0..31) {
                for (y in 0..31) {
                    for (z in 0..31) {
                        cellData = rawChunkData[x * 32 * 32 + y * 32 + z]
                        val voxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(cellData))!!

                        cell.x = (chunk.chunkX shl 5) + x
                        cell.y = (chunk.chunkY shl 5) + y
                        cell.z = (chunk.chunkZ shl 5) + z

                        cell.voxel = voxel
                        cell.metaData = VoxelFormat.meta(cellData)
                        cell.sunlight = VoxelFormat.sunlight(cellData)
                        cell.blocklight = VoxelFormat.blocklight(cellData)

                        fun shouldRenderFace(neighborData: Int, face: UnitCube.CubeFaceData, side: VoxelSide): Boolean {
                            val neighborVoxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(neighborData))!!
                            if (opaque(neighborVoxel) || (voxel == neighborVoxel && voxel.selfOpaque))
                                return false
                            return true
                        }

                        if (!voxel.isAir()) {
                            val routine = voxel.customRenderingRoutine
                            if (routine != null) {
                                mesher.let {
                                    it.x = x
                                    it.y = y
                                    it.z = z
                                }
                                routine.invoke(mesher, cell)
                            } else {
                                val materialTagName = if (voxel.name == "water") "water" else "opaque"

                                val scratch = map.getOrPut(materialTagName) { ScratchBuffer() }
                                val meshData = scratch.meshData
                                val cubesData = scratch.cubesData

                                fun face(neighborData: Int, face: UnitCube.CubeFaceData, side: VoxelSide) {
                                    val neighborVoxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(neighborData))!!
                                    if (opaque(neighborVoxel) || (voxel == neighborVoxel && voxel.selfOpaque))
                                        return

                                    val voxelTexture = voxel.getVoxelTexture(cell, side) as VoxelTexturesArray.VoxelTextureInArray

                                    //val textureName = "voxels/textures/"+voxelTexture.name.replace('.','/')+".png"
                                    //val textureId = (backend.textures[textureName] as VulkanTexture2D).mapping
                                    val textureId = voxelTexture.textureArrayIndex

                                    val sunlight = VoxelFormat.sunlight(neighborData)
                                    val blocklight = max(VoxelFormat.blocklight(neighborData), voxel.emittedLightLevel)

                                    for ((vertex, texcoord) in face.vertices) {
                                        /*meshData.put((vertex[0] + x).toByte())
                                        meshData.put((vertex[1] + y).toByte())
                                        meshData.put((vertex[2] + z).toByte())
                                        meshData.put(0)*/
                                        meshData.putFloat(vertex[0] + x)
                                        meshData.putFloat(vertex[1] + y)
                                        meshData.putFloat(vertex[2] + z)

                                        meshData.put((sunlight * 16).toByte())
                                        meshData.put((blocklight * 16).toByte())
                                        meshData.put(0)
                                        meshData.put(0)

                                        meshData.put(face.normalDirection.x().toSNORM())
                                        meshData.put(face.normalDirection.y().toSNORM())
                                        meshData.put(face.normalDirection.z().toSNORM())
                                        meshData.put(0)

                                        meshData.putShort(texcoord[0].toUNORM16())
                                        meshData.putShort(texcoord[1].toUNORM16())

                                        meshData.putInt(textureId)
                                        scratch.meshTriCount++
                                    }
                                }

                                fun cube() {
                                    face(data(x, y - 1, z), UnitCube.bottomFace, VoxelSide.BOTTOM)
                                    face(data(x, y + 1, z), UnitCube.topFace, VoxelSide.TOP)

                                    face(data(x - 1, y, z), UnitCube.leftFace, VoxelSide.LEFT)
                                    face(data(x + 1, y, z), UnitCube.rightFace, VoxelSide.RIGHT)

                                    face(data(x, y, z - 1), UnitCube.backFace, VoxelSide.BACK)
                                    face(data(x, y, z + 1), UnitCube.frontFace, VoxelSide.FRONT)
                                }

                                cube()

                                /*if (shouldRenderFace(data(x, y - 1, z), UnitCube.bottomFace, VoxelSide.BOTTOM) ||
                                        shouldRenderFace(data(x, y + 1, z), UnitCube.topFace, VoxelSide.TOP) ||
                                        shouldRenderFace(data(x - 1, y, z), UnitCube.leftFace, VoxelSide.LEFT) ||
                                        shouldRenderFace(data(x + 1, y, z), UnitCube.rightFace, VoxelSide.RIGHT) ||
                                        shouldRenderFace(data(x, y, z - 1), UnitCube.backFace, VoxelSide.BACK) ||
                                        shouldRenderFace(data(x, y, z + 1), UnitCube.frontFace, VoxelSide.FRONT)) {

                                    cubesData.put(x.toByte())
                                    cubesData.put(y.toByte())
                                    cubesData.put(z.toByte())
                                    cubesData.put(0)

                                    cubesData.putInt(0)
                                    scratch.cubesCount++
                                }*/
                            }
                        }
                    }
                }
            }
        }

        val sections = map.filter { it.value.cubesCount > 0 || it.value.meshTriCount > 0 }.mapValues {
            val scratch = it.value

            fun buf2vk(buffer: ByteBuffer): VulkanVertexBuffer {
                buffer.flip()
                val vertexBuffer = VulkanVertexBuffer(backend, buffer.limit().toLong(), MemoryUsagePattern.SEMI_STATIC)
                vertexBuffer.upload(buffer)
                return vertexBuffer
            }

            val cubes = if (scratch.cubesCount > 0) ChunkRepresentation.Section.CubesInstances(buf2vk(scratch.cubesData), scratch.cubesCount) else null
            val staticMesh = if (scratch.meshTriCount > 0) ChunkRepresentation.Section.StaticMesh(buf2vk(scratch.meshData), scratch.meshTriCount) else null

            ChunkRepresentation.Section(it.key, cubes, staticMesh)
        }

        map.values.forEach(Cleanable::cleanup)

        receiver.acceptNewData(sections)
        return true
    }

    companion object {
        val neighborsIndexes = generateNeighbors()

        fun generateNeighbors(): List<Triple<Int, Int, Int>> {
            val list = mutableListOf<Triple<Int, Int, Int>>()

            for (x in -1..1)
                for (y in -1..1)
                    for (z in -1..1)
                        list += Triple(x, y, z)

            return list.filterNot { (x, y, z) -> (x == 0 && y == 0 && z == 0) }
        }
    }
}

private fun Int.clamp(min: Int, max: Int) = max(min, min(this, max))

fun Float.toSNORM(): Byte = ((this + 0.0f) * 0.5f * 255f).toInt().clamp(-128, 127).toByte()
fun Float.toUNORM16(): Short = (this * 65535f).toInt().clamp(0, 65535).toShort()