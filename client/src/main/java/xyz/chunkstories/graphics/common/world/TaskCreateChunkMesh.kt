@file:Suppress("NOTHING_TO_INLINE")

package xyz.chunkstories.graphics.common.world

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.util.kotlin.getNormalMatrix
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.cell.PodCellData
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.UnitCube
import xyz.chunkstories.gui.logger
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.*

abstract class TaskCreateChunkMesh(
        val chunk: ChunkImplementation, attachedProperty: AutoRebuildingProperty, updates: Int,
        val done: (Map<String, ScratchBuffer>) -> Unit

) : AutoRebuildingProperty.UpdateTask(attachedProperty, updates) {
    val dummyData = PodCellData(chunk.world.content.blockTypes.air)

    inline fun opaque(voxel: BlockType) = voxel.opaque
    inline fun opaque(x2: Int, y2: Int, z2: Int): Boolean = opaque(data(x2, y2, z2).blockType)

    inline fun data(x2: Int, y2: Int, z2: Int): CellData =
            if (x2 in 0..31 && y2 in 0..31 && z2 in 0..31)
                chunk.getCell(x2, y2, z2).data //rawChunkData[x2 * 32 * 32 + y2 * 32 + z2]
            else
                chunk.world.getCell(x2 + chunk.chunkX * 32, y2 + chunk.chunkY * 32, z2 + chunk.chunkZ * 32)?.data ?: dummyData

    class ScratchBuffer : Cleanable {
        val cubesData: ByteBuffer = MemoryUtil.memAlloc(1024 * 1024 * 4 * 4)
        var cubesCount = 0
        val meshData: ByteBuffer = MemoryUtil.memAlloc(1024 * 1024 * 4 * 4)
        var meshTriCount = 0
        override fun cleanup() {
            MemoryUtil.memFree(cubesData)
            MemoryUtil.memFree(meshData)
        }
    }

    override fun update(taskExecutor: TaskExecutor): Boolean {
        if (chunk.holder().state !is ChunkHolder.State.Available)
            return true

        val neighborsPresent = neighborsIndexes.count { (x, y, z) ->
            val neighbor = chunk.world.chunksManager.getChunk(chunk.chunkX + x, chunk.chunkY + y, chunk.chunkZ + z)
            (neighbor != null || (chunk.chunkY + y < 0) || (chunk.chunkY + y >= chunk.world.properties.size.heightInChunks))
        }
        if (neighborsPresent < neighborsIndexes.size)
            return true

        val rng = Random(1)

        val map = mutableMapOf<String, ScratchBuffer>()

        if (chunk.isAirChunk) {

        } else {

            // val cell = ScratchCell(chunk.world)
            // var cellData = 0
            lateinit var cell: Cell
            // var cellData: CellData = PodCellData(chunk.world.content.blockTypes.air)

            val mesher = object : BlockRepresentation.Custom.RenderInterface {
                var x = 0; var y = 0; var z = 0

                override fun addModel(model: Model, matrix: Matrix4f?, materialsOverrides: Map<Int, MeshMaterial>) {
                    val sunlight = cell.data.sunlightLevel
                    val blocklight = cell.data.blocklightLevel

                    var ox = x
                    var oy = y
                    var oz = z

                    val normalMatrix = matrix?.getNormalMatrix()
                    val vertex = Vector4f()
                    val normal = Vector3f()

                    for ((index, mesh) in model.meshes.withIndex()) {
                        val material = materialsOverrides[index] ?: mesh.material
                        val scratch = map.getOrPut(material.tag) { ScratchBuffer() }
                        val meshData = scratch.meshData
                        var texName = material.textures["albedoTexture"] ?: "notex"

                        // TODO move to just referencing the actual tex
                        when {
                            texName.startsWith("voxels/blockmodels") -> {
                                assert(false)
                                texName = texName.removePrefix("voxels/blockmodels/").substringAfter("/", "notex") ?: "notex"
                            }
                            texName.startsWith("voxels/textures") -> {
                                assert(false)
                                texName = texName.removePrefix("voxels/textures/") ?: "notex"
                            }
                        }
                        assert(!texName.endsWith(".png"))
                        texName = texName.removeSuffix(".png")

                        val voxelTexture = chunk.world.content.blockTypes.textures.getTextureOrDefault(texName)
                        val textureId = voxelTexture.id
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
                            meshData.put(1f.toUNORM8())
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
                            meshData.putInt(0)
                            scratch.meshTriCount++
                        }
                    }
                }
            }

            try {
                for (x in 0..31) {
                    for (y in 0..31) {
                        for (z in 0..31) {
                            cell = chunk.getCell(x, y, z) // rawChunkData[x * 32 * 32 + y * 32 + z]

                            val voxel = cell.data.blockType

                            /*cell.x = (chunk.chunkX shl 5) + x
                            cell.y = (chunk.chunkY shl 5) + y
                            cell.z = (chunk.chunkZ shl 5) + z*/

                            /*cell.voxel = voxel
                            cell.metaData = VoxelFormat.meta(cellData)
                            cell.sunlight = VoxelFormat.sunlight(cellData)
                            cell.blocklight = VoxelFormat.blocklight(cellData)*/

                            /*fun shouldRenderFace(neighborData: Int, face: UnitCube.CubeFaceData, side: VoxelSide): Boolean {
                            val neighborVoxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(neighborData))!!
                            if (opaque(neighborVoxel) || (voxel == neighborVoxel && voxel.selfOpaque))
                                return false
                            return true
                            }*/

                            if (!voxel.isAir) {
                                val routine = voxel.representation
                                if (routine is BlockRepresentation.Custom) {
                                    mesher.let {
                                        it.x = x
                                        it.y = y
                                        it.z = z
                                    }
                                    routine.drawRoutine(mesher, cell)
                                } else {
                                    val materialTagName = if (voxel.name == "water") "water" else "opaque"

                                    val scratch = map.getOrPut(materialTagName) { ScratchBuffer() }
                                    val meshData = scratch.meshData
                                    val cubesData = scratch.cubesData

                                    fun face(neighborData: CellData, face: UnitCube.CubeFaceData, side: BlockSide) {
                                        val neighborBlockType = neighborData.blockType
                                        if (opaque(neighborBlockType) || (voxel == neighborBlockType && voxel.selfOpaque))
                                            return

                                        val voxelTexture = voxel.getTexture(cell, side)
                                        val textureId = voxelTexture.id

                                        val sunlight = neighborData.sunlightLevel
                                        val blocklight = Integer.max(neighborData.blocklightLevel, voxel.emittedLightLevel)

                                        // compute AO
                                        val aoArray = arrayOf(0.0f, 0.5f, 0.75f, 1.0f)
                                        //val aoArray = arrayOf(1f, 1f, 1f, 1.0f)

                                        fun vertexAo(side1: Boolean, side2: Boolean, corner: Boolean): Float = when {
                                            side1 && side2 -> 0.25f
                                            !side1 && side2 && corner -> 0.5f
                                            side1 && !side2 && corner -> 0.5f
                                            !side1 && !side2 && !corner -> 1f
                                            else -> 0.75f
                                        }

                                        val opaquenessNearby = BooleanArray(9)
                                        fun ao(ds: Int, dt: Int): Float {
                                            val side1 = opaquenessNearby[((1 + ds) * 3) + (1)]
                                            val side2 = opaquenessNearby[((1) * 3) + (1 + dt)]
                                            val corner = opaquenessNearby[((1 + ds) * 3) + (1 + dt)]
                                            return vertexAo(side1, side2, corner)
                                        }

                                        when (side) {
                                            BlockSide.TOP -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x + s, y + 1, z + t)
                                                    }
                                                }
                                            }
                                            BlockSide.BOTTOM -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x + s, y - 1, z + t)
                                                    }
                                                }
                                            }
                                            BlockSide.LEFT -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x - 1, y + s, z + t)
                                                    }
                                                }
                                            }
                                            BlockSide.RIGHT -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x + 1, y + s, z + t)
                                                    }
                                                }
                                            }
                                            BlockSide.FRONT -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x + s, y + t, z + 1)
                                                    }
                                                }
                                            }
                                            BlockSide.BACK -> {
                                                for (s in -1..1) {
                                                    for (t in -1..1) {
                                                        val cs = s + 1
                                                        val ct = t + 1

                                                        opaquenessNearby[cs * 3 + ct] = opaque(x + s, y + t, z - 1)
                                                    }
                                                }
                                            }
                                        }

                                        aoArray[0] = ao(-1, -1)
                                        aoArray[1] = ao(1, -1)
                                        aoArray[2] = ao(-1, 1)
                                        aoArray[3] = ao(1, 1)

                                        var i = 0
                                        for ((vertex, texcoord, aoIndex) in face.vertices) {
                                            /*meshData.put((vertex[0] + x).toByte())
                                        meshData.put((vertex[1] + y).toByte())
                                        meshData.put((vertex[2] + z).toByte())
                                        meshData.put(0)*/
                                            meshData.putFloat(vertex[0] + x)
                                            meshData.putFloat(vertex[1] + y)
                                            meshData.putFloat(vertex[2] + z)

                                            meshData.put((sunlight * 16).toByte())
                                            meshData.put((blocklight * 16).toByte())
                                            meshData.put(aoArray[aoIndex].toUNORM8())
                                            meshData.put(0)

                                            meshData.put(face.normalDirection.x().toSNORM())
                                            meshData.put(face.normalDirection.y().toSNORM())
                                            meshData.put(face.normalDirection.z().toSNORM())
                                            meshData.put(0)

                                            meshData.putShort(texcoord[0].toUNORM16())
                                            meshData.putShort(texcoord[1].toUNORM16())

                                            meshData.putInt(textureId)
                                            meshData.putInt(0)
                                            scratch.meshTriCount++
                                            i++
                                        }
                                    }

                                    fun cube() {
                                        face(data(x, y - 1, z), UnitCube.bottomFace, BlockSide.BOTTOM)
                                        face(data(x, y + 1, z), UnitCube.topFace, BlockSide.TOP)

                                        face(data(x - 1, y, z), UnitCube.leftFace, BlockSide.LEFT)
                                        face(data(x + 1, y, z), UnitCube.rightFace, BlockSide.RIGHT)

                                        face(data(x, y, z - 1), UnitCube.backFace, BlockSide.BACK)
                                        face(data(x, y, z + 1), UnitCube.frontFace, BlockSide.FRONT)
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
            } catch(boe: BufferOverflowException) {
                logger.warn("Too much geometry")
            }
        }

        val sections = map.filter { it.value.cubesCount > 0 || it.value.meshTriCount > 0 }

        done(sections)

        map.values.forEach(Cleanable::cleanup)

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

private fun Int.clamp(min: Int, max: Int) = Integer.max(min, Integer.min(this, max))

fun Float.toSNORM(): Byte = ((this + 0.0f) * 0.5f * 255f).toInt().clamp(-128, 127).toByte()

fun Float.toUNORM8(): Byte = (this * 255f).toInt().clamp(0, 255).toByte()
fun Float.toUNORM16(): Short = (this * 65535f).toInt().clamp(0, 65535).toShort()