//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.overlaps
import xyz.chunkstories.api.world.WorldCollisionsManager
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.world.iterators.EntityRayIterator
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.*

/** Responsible for handling the 'pixel-perfect' AABB collisions  */
class DefaultWorldCollisionsManager(private val world: WorldImplementation) : WorldCollisionsManager {

    internal val lock = ReentrantLock()

    override fun raytraceSolid(initialPosition: Vector3dc, direction: Vector3dc, limit: Double): Location? {
        return raytraceSolid(initialPosition, direction, limit, false, false)
    }

    override fun raytraceSolidOuter(initialPosition: Vector3dc, direction: Vector3dc, limit: Double): Location? {
        return raytraceSolid(initialPosition, direction, limit, true, false)
    }

    override fun raytraceSelectable(initialPosition: Location, direction: Vector3dc, limit: Double): Location? {
        return raytraceSolid(initialPosition, direction, limit, false, true)
    }

    private fun raytraceSolid(initialPosition: Vector3dc, directionIn: Vector3dc, limit: Double, outer: Boolean,
                              selectable: Boolean): Location? {
        val direction = Vector3d()
        directionIn.normalize(direction)

        var cell: Cell
        var x = floor(initialPosition.x()).toInt()
        var y = floor(initialPosition.y()).toInt()
        var z = floor(initialPosition.z()).toInt()

        // DDA algorithm

        // It requires double arrays because it works using loops over each dimension
        val rayOrigin = DoubleArray(3)
        val rayDirection = DoubleArray(3)
        rayOrigin[0] = initialPosition.x()
        rayOrigin[1] = initialPosition.y()
        rayOrigin[2] = initialPosition.z()
        rayDirection[0] = direction.x()
        rayDirection[1] = direction.y()
        rayDirection[2] = direction.z()
        val voxelCoords = intArrayOf(x, y, z)
        val voxelDelta = intArrayOf(0, 0, 0)
        val deltaDist = DoubleArray(3)
        val next = DoubleArray(3)
        val step = IntArray(3)

        var side : Int
        // Prepare distances
        for (i in 0..2) {
            val deltaX = rayDirection[0] / rayDirection[i]
            val deltaY = rayDirection[1] / rayDirection[i]
            val deltaZ = rayDirection[2] / rayDirection[i]
            deltaDist[i] = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
            if (rayDirection[i] < 0f) {
                step[i] = -1
                next[i] = (rayOrigin[i] - voxelCoords[i]) * deltaDist[i]
            } else {
                step[i] = 1
                next[i] = (voxelCoords[i] + 1f - rayOrigin[i]) * deltaDist[i]
            }
        }

        do {

            // DDA steps
            side = 0
            for (i in 1..2) {
                if (next[side] > next[i]) {
                    side = i
                }
            }
            next[side] += deltaDist[side]
            voxelCoords[side] += step[side]
            voxelDelta[side] += step[side]

            x = voxelCoords[0]
            y = voxelCoords[1]
            z = voxelCoords[2]
            cell = world.peek(x, y, z)
            val voxel = cell.voxel

            if (voxel.solid || (selectable && voxel.liquid)) {
                if (voxel.isAir())
                    continue

                var collides = false
                for (box in cell.translatedCollisionBoxes ?: emptyArray()) {
                    // System.out.println(box);
                    val collisionPoint = box.lineIntersection(initialPosition, direction)
                    if (collisionPoint != null) {
                        collides = true
                        // System.out.println("collides @ "+collisionPoint);
                    }
                }
                if (collides) {
                    if (!outer)
                        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    else {
                        // Back off a bit
                        when (side) {
                            0 -> x -= step[side]
                            1 -> y -= step[side]
                            2 -> z -= step[side]
                        }
                        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    }
                }
            }

            // distance += deltaDist[side];

        } while (voxelDelta[0] * voxelDelta[0] + voxelDelta[1] * voxelDelta[1] + voxelDelta[2] * voxelDelta[2] < limit * limit)
        return null
    }

    override fun rayTraceEntities(initialPosition: Vector3dc, direction: Vector3dc, limit: Double): Iterator<Entity> {
        var blocksLimit = limit

        val blocksCollision = this.raytraceSolid(initialPosition, direction, limit)
        if (blocksCollision != null)
            blocksLimit = blocksCollision.distance(initialPosition)

        return raytraceEntitiesIgnoringVoxels(initialPosition, direction, min(blocksLimit, limit))
    }

    override fun raytraceEntitiesIgnoringVoxels(initialPosition: Vector3dc, direction: Vector3dc,
                                                limit: Double): Iterator<Entity> {
        return EntityRayIterator(world, initialPosition, direction, limit)
    }

    /**
     * Does a complicated check to see how far the entity can go using the delta
     * direction, from the 'start' position. Does not actually move anything Returns
     * the remaining distance in each dimension if it got stuck ( with vec3(0.0,
     * 0.0, 0.0) meaning it can safely move without colliding with anything )
     */
    override fun runEntityAgainstWorldVoxels(entity: Entity, from: Vector3dc, delta: Vector3dc): Vector3d {
        return runEntityAgainst(entity, from, delta, false)
    }

    override fun runEntityAgainstWorldVoxelsAndEntities(entity: Entity, from: Vector3dc, delta: Vector3dc): Vector3d {
        return runEntityAgainst(entity, from, delta, true)
    }

    override fun tryMovingEntityWithCollisions(entity: Entity, from: Vector3dc, delta: Vector3dc): Vector3dc {
        try {
            lock.lock()
            val travel = Vector3d(delta)
            val blocked = runEntityAgainst(entity, from, delta, true)
            travel.sub(blocked)
            entity.traits.with(TraitCollidable::class) { moveWithCollisionRestrain(travel) }
            return blocked
        } finally {
            lock.unlock()
        }
    }

    fun runEntityAgainst(entity: Entity, from: Vector3dc, delta: Vector3dc, collideWithEntities: Boolean): Vector3d {
        var cell: Cell
        val boxes = ArrayList<Box>()

        // Extract the current position
        val pos = Vector3d(from)

        // Keep biggest distanceToTravel for each dimension Box of our entity
        val maxDistanceToTravel = Vector3d(0.0)

        val direction = Vector3d(delta)
        direction.normalize()

        val traitCollisions = entity.traits[TraitCollidable::class.java] ?: return Vector3d(delta)

        // Iterate over every box
        for (collisionBoxIndex in 0 until traitCollisions.collisionBoxes.size) {
            // Make a normalized double vector and keep the original length
            var vec = Vector3d(delta)
            val distanceToTravel = Vector3d(delta)
            val len = vec.length()
            vec.normalize()
            vec.mul(0.25)

            // Do it block per block, face per face
            var distanceTraveled = 0.0
            var checkerX: Box
            var checkerY: Box
            var checkerZ: Box

            val checkerMin = traitCollisions.collisionBoxes[collisionBoxIndex].min
            val checkerMax = traitCollisions.collisionBoxes[collisionBoxIndex].max

            var stepDistanceX: Double
            var stepDistanceY: Double
            var stepDistanceZ: Double

            val entitiesCenter = Vector3d(pos.x(), pos.y(), pos.z())

            val entitiesRadius = Vector3d(traitCollisions.collisionBoxes[collisionBoxIndex].extents.x() * 5,
                    traitCollisions.collisionBoxes[collisionBoxIndex].extents.y() * 5 + 10.0, traitCollisions.collisionBoxes[collisionBoxIndex].extents.z() * 5)

            while (distanceTraveled < len) {
                if (len - distanceTraveled > 0.25) {
                    // DistanceTraveled is incremented no matter what, for momentum loss while
                    // sliding on walls
                    distanceTraveled += 0.25
                } else {
                    vec = Vector3d(delta)
                    vec.normalize()
                    vec.mul(len - distanceTraveled)
                    distanceTraveled = len
                }

                stepDistanceX = vec.x()
                stepDistanceY = vec.y()
                stepDistanceZ = vec.z()

                if (delta.z() != 0.0) {
                    boxes.clear()
                    checkerZ = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x(), pos.y(), pos.z() + stepDistanceZ)
                    for (i in floor(pos.x() + checkerMin.x).toInt() - 1 until ceil(pos.x() + checkerMax.x).toInt() + 1) {
                        for (j in floor(pos.y() + checkerMin.y).toInt() - 1 until ceil(pos.y() + checkerMax.y).toInt() + 1) {
                            for (k in floor(pos.z() + checkerMin.z).toInt() - 1 until ceil(pos.z() + checkerMax.z).toInt() + 1) {
                                cell = world.peek(i, j, k)
                                if (cell.voxel.solid)
                                    addAllSafe(boxes, cell.translatedCollisionBoxes)
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(Box.fromExtentsCentered(entitiesRadius).translate(entitiesCenter)).forEach { e ->
                            if (e !== entity)
                                addAllSafeAndTranslate(boxes, e, e.location)
                        }

                    for (box in boxes) {
                        if (overlaps(checkerZ, box)) {
                            stepDistanceZ = 0.0
                            if (delta.z() < 0) {
                                //val south = min(box.zPosition + box.zWidth + checkerZ.zWidth - pos.z(), 0.0)
                                val south =box.max.z - (pos.z() + checkerMin.z)
                                stepDistanceZ = south
                            } else {
                                //val north = max(box.zPosition - (pos.z() + checkerZ.zWidth), 0.0)
                                val north = box.min.z - (pos.z + checkerMax.x)
                                stepDistanceZ = north
                            }
                            vec.z = 0.0
                            checkerZ = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x(), pos.y(), pos.z() + stepDistanceZ)
                        }
                    }

                    distanceToTravel.z = distanceToTravel.z() - stepDistanceZ
                    pos.z = pos.z() + stepDistanceZ
                }

                if (delta.x() != 0.0) {
                    boxes.clear()
                    checkerX = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x() + stepDistanceX, pos.y(), pos.z())
                    for (i in floor(pos.x() + checkerMin.x).toInt() - 1 until ceil(pos.x() + checkerMax.x).toInt() + 1) {
                        for (j in floor(pos.y() + checkerMin.y).toInt() - 1 until ceil(pos.y() + checkerMax.y).toInt() + 1) {
                            for (k in floor(pos.z() + checkerMin.z).toInt() - 1 until ceil(pos.z() + checkerMax.z).toInt() + 1) {
                                cell = world.peek(i, j, k)
                                if (cell.voxel.solid)
                                    addAllSafe(boxes, cell.translatedCollisionBoxes)
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(Box.fromExtentsCentered(entitiesRadius).translate(entitiesCenter)).forEach { e -> if (e !== entity) addAllSafeAndTranslate(boxes, e, e.location) }

                    for (box in boxes) {
                        if (overlaps(checkerX, box)) {
                            if (delta.x() < 0) {
                                val left = box.max.x - (pos.x + checkerMin.x)
                                stepDistanceX = left
                            } else {
                                val right2 = box.min.x - (pos.x() + checkerMax.x)
                                stepDistanceX = right2
                            }
                            vec.x = 0.0
                            checkerX = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x() + stepDistanceX, pos.y(), pos.z())
                        }

                    }

                    pos.x += stepDistanceX
                    distanceToTravel.x -= stepDistanceX
                }

                if (delta.y() != 0.0) {
                    boxes.clear()
                    checkerY = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x(), pos.y() + stepDistanceY, pos.z())
                    for (i in floor(pos.x() + checkerMin.x).toInt() - 1 until ceil(pos.x() + checkerMax.x).toInt() + 1) {
                        for (j in floor(pos.y() + checkerMin.y).toInt() - 1 until ceil(pos.y() + checkerMax.y).toInt() + 1) {
                            for (k in floor(pos.z() + checkerMin.z).toInt() - 1 until ceil(pos.z() + checkerMax.z).toInt() + 1) {
                                cell = world.peek(i, j, k)
                                if (cell.voxel.solid)
                                    addAllSafe(boxes, cell.translatedCollisionBoxes)
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(Box.fromExtentsCentered(entitiesRadius).translate(entitiesCenter)).forEach { e -> if (e !== entity) addAllSafeAndTranslate(boxes, e, e.location) }

                    for (box in boxes) {
                        if (overlaps(checkerY, box)) {
                            if (delta.y() < 0) {
                                val top = box.max.y - (pos.y() + checkerMin.y)
                                stepDistanceY = top
                            } else {
                                //val bot = max(box.yPosition - (pos.y() + checkerY.yHeight), 0.0)
                                val bot = box.min.y - (pos.y() + checkerMax.y)
                                stepDistanceY = bot
                            }
                            vec.y = 0.0
                            checkerY = traitCollisions.collisionBoxes[collisionBoxIndex].translate(pos.x(), pos.y() + stepDistanceY, pos.z())
                        }

                    }

                    pos.y += stepDistanceY
                    distanceToTravel.y -= stepDistanceY
                }
            }

            if (abs(distanceToTravel.x()) > abs(maxDistanceToTravel.x()))
                maxDistanceToTravel.x = distanceToTravel.x()

            if (abs(distanceToTravel.y()) > abs(maxDistanceToTravel.y()))
                maxDistanceToTravel.y = distanceToTravel.y()

            if (abs(distanceToTravel.z()) > abs(maxDistanceToTravel.z()))
                maxDistanceToTravel.z = distanceToTravel.z()
        }
        return maxDistanceToTravel
    }

    private inline fun addAllSafeAndTranslate(list: ArrayList<Box>, e: Entity, location: Location) {
        e.traits[TraitCollidable::class.java]?.apply {
            if (!collidesWithEntities)
                return@apply

            for (box in collisionBoxes) {
                box.translate(location)
                list.add(box)
            }
        }
    }

    private inline fun <T> addAllSafe(l: ArrayList<T>, e: Array<T>?) {
        if (e != null)
            Collections.addAll(l, *e)
    }

    override fun isPointSolid(point: Vector3dc): Boolean {
        val peek = world.peek(point)

        if (peek.voxel.solid) {
            // Fast check if the voxel is just a solid block
            // TODO isOpaque doesn't mean that exactly, newEntity a new type variable that
            // represents that specific trait
            if (peek.voxel.opaque)
                return true

            // Else iterate over each box that make up that block
            val boxes = peek.voxel.getTranslatedCollisionBoxes(peek)
            if (boxes != null)
                for (box in boxes)
                    if (box.isPointInside(point))
                        return true

        }
        return false
    }
}
