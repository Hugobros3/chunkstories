//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

import org.joml.Vector3d
import org.joml.Vector3dc

import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.entity.traits.TraitCollidable
import io.xol.chunkstories.api.physics.Box
import io.xol.chunkstories.api.world.WorldCollisionsManager
import io.xol.chunkstories.api.world.cell.CellData
import io.xol.chunkstories.world.iterators.EntityRayIterator

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

        var cell: CellData
        var x: Int
        var y: Int
        var z: Int
        x = Math.floor(initialPosition.x()).toInt()
        y = Math.floor(initialPosition.y()).toInt()
        z = Math.floor(initialPosition.z()).toInt()

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

        var side = 0
        // Prepare distances
        for (i in 0..2) {
            val deltaX = rayDirection[0] / rayDirection[i]
            val deltaY = rayDirection[1] / rayDirection[i]
            val deltaZ = rayDirection[2] / rayDirection[i]
            deltaDist[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
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
            cell = world.peekSafely(x, y, z)
            if (cell.getVoxel()!!.solid || selectable) {
                var collides = false
                for (box in cell.getTranslatedCollisionBoxes()!!) {
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

        return raytraceEntitiesIgnoringVoxels(initialPosition, direction, Math.min(blocksLimit, limit))
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

            entity.traits.with<TraitCollidable>(TraitCollidable::class) { moveWithCollisionRestrain(travel) }
            return blocked
        } finally {
            lock.unlock()
        }
    }

    fun runEntityAgainst(entity: Entity, from: Vector3dc, delta: Vector3dc, collideWithEntities: Boolean): Vector3d {
        var cell: CellData
        val boxes = ArrayList<Box>()

        // Extract the current position
        val pos = Vector3d(from)

        // Keep biggest distanceToTravel for each dimension Box of our entity
        val maxDistanceToTravel = Vector3d(0.0)

        val direction = Vector3d(delta)
        direction.normalize()

        val e_coli = entity.traits[TraitCollidable::class.java] ?: return Vector3d(delta)

        // Iterate over every box
        for (eCB in 0 until e_coli.collisionBoxes.size) {
            // Make a normalized double vector and keep the original length
            var vec = Vector3d(delta)
            val distanceToTravel = Vector3d(delta)
            val len = vec.length()
            vec.normalize()
            vec.mul(0.25)

            // Do it block per block, face per face
            var distanceTraveled = 0.0
            var checkerX = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y(), pos.z())
            var checkerY = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y(), pos.z())
            var checkerZ = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y(), pos.z())

            var stepDistanceX: Double
            var stepDistanceY: Double
            var stepDistanceZ: Double

            val entitiesCenter = Vector3d(pos.x() - e_coli.collisionBoxes[eCB].xWidth,
                    pos.y() - e_coli.collisionBoxes[eCB].yHeight - 2.0, pos.z() - e_coli.collisionBoxes[eCB].zWidth)
            val entitiesRadius = Vector3d(e_coli.collisionBoxes[eCB].xWidth * 5,
                    e_coli.collisionBoxes[eCB].yHeight * 5, e_coli.collisionBoxes[eCB].zWidth * 5)

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
                    checkerZ = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y(), pos.z() + stepDistanceZ)
                    for (i in Math.floor(pos.x()).toInt() - 1 until Math.ceil(pos.x() + checkerX.xWidth).toInt()) {
                        for (j in Math.floor(pos.y()).toInt() - 1 until Math.ceil(pos.y() + checkerX.yHeight).toInt()) {
                            for (k in Math.floor(pos.z()).toInt() - 1 until Math
                                    .ceil(pos.z() + checkerX.zWidth).toInt()) {
                                cell = world.peekSafely(i, j, k)
                                if (cell.getVoxel()!!.solid)
                                    addAllSafe(boxes, cell.getTranslatedCollisionBoxes())
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(entitiesCenter, entitiesRadius).forEach { e ->
                            if (e !== entity)
                                addAllSafeAndTranslate(boxes, e, e.location)
                        }

                    for (box in boxes) {
                        if (checkerZ.collidesWith(box)) {
                            stepDistanceZ = 0.0
                            if (delta.z() < 0) {
                                val south = Math.min(box.zPosition + box.zWidth + checkerZ.zWidth - pos.z(), 0.0)
                                stepDistanceZ = south
                            } else {
                                val north = Math.max(box.zPosition - (pos.z() + checkerZ.zWidth), 0.0)
                                stepDistanceZ = north
                            }
                            vec.z = 0.0
                            checkerZ = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y(),
                                    pos.z() + stepDistanceZ)
                        }
                    }

                    distanceToTravel.z = distanceToTravel.z() - stepDistanceZ
                    pos.z = pos.z() + stepDistanceZ
                }

                if (delta.x() != 0.0) {
                    boxes.clear()
                    checkerX = e_coli.collisionBoxes[eCB].translate(pos.x() + stepDistanceX, pos.y(), pos.z())
                    for (i in Math.floor(pos.x()).toInt() - 1 until Math.ceil(pos.x() + checkerY.xWidth).toInt()) {
                        for (j in Math.floor(pos.y()).toInt() - 1 until Math.ceil(pos.y() + checkerY.yHeight).toInt()) {
                            for (k in Math.floor(pos.z()).toInt() - 1 until Math
                                    .ceil(pos.z() + checkerY.zWidth).toInt()) {
                                cell = world.peekSafely(i, j, k)
                                if (cell.getVoxel()!!.solid)
                                    addAllSafe(boxes, cell.getTranslatedCollisionBoxes())
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(entitiesCenter, entitiesRadius).forEach { e ->
                            if (e !== entity) {
                                addAllSafeAndTranslate(boxes, e, e.location)
                            }
                        }

                    for (box in boxes) {
                        if (checkerX.collidesWith(box)) {
                            stepDistanceX = 0.0
                            if (delta.x() < 0) {
                                val left = Math.min(box.xPosition + box.xWidth + checkerX.xWidth - pos.x(), 0.0)
                                // System.out.println("left:"+left);
                                stepDistanceX = left
                            } else {
                                val right = Math.max(box.xPosition - (pos.x() + checkerX.xWidth), 0.0)
                                // System.out.println("right"+right);
                                stepDistanceX = right
                            }
                            vec.x = 0.0
                            checkerX = e_coli.collisionBoxes[eCB].translate(pos.x() + stepDistanceX, pos.y(),
                                    pos.z())
                        }

                    }

                    pos.x = pos.x() + stepDistanceX
                    distanceToTravel.x = distanceToTravel.x() - stepDistanceX
                }

                if (delta.y() != 0.0) {
                    boxes.clear()
                    checkerY = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y() + stepDistanceY, pos.z())
                    for (i in Math.floor(pos.x()).toInt() - 1 until Math.ceil(pos.x() + checkerZ.xWidth).toInt()) {
                        for (j in Math.floor(pos.y()).toInt() - 1 until Math.ceil(pos.y() + checkerZ.yHeight
                                + 1.0).toInt()) {
                            for (k in Math.floor(pos.z()).toInt() - 1 until Math
                                    .ceil(pos.z() + checkerZ.zWidth).toInt()) {
                                cell = world.peekSafely(i, j, k)
                                if (cell.getVoxel()!!.solid)
                                    addAllSafe(boxes, cell.getTranslatedCollisionBoxes())
                            }
                        }
                    }

                    if (collideWithEntities)
                        world.getEntitiesInBox(entitiesCenter, entitiesRadius).forEach { e ->
                            if (e !== entity)
                                addAllSafeAndTranslate(boxes, e, e.location)
                        }

                    for (box in boxes) {
                        if (checkerY.collidesWith(box)) {
                            stepDistanceY = 0.0
                            if (delta.y() < 0) {
                                val top = Math.min(box.yPosition + box.yHeight - pos.y(), 0.0)
                                // System.out.println(top);
                                stepDistanceY = top
                            } else {
                                val bot = Math.max(box.yPosition - (pos.y() + checkerY.yHeight), 0.0)
                                // System.out.println(bot);
                                stepDistanceY = bot
                            }
                            vec.y = 0.0
                            checkerY = e_coli.collisionBoxes[eCB].translate(pos.x(), pos.y() + stepDistanceY,
                                    pos.z())
                        }

                    }

                    pos.y = pos.y() + stepDistanceY
                    distanceToTravel.y = distanceToTravel.y() - stepDistanceY
                }
            }

            if (Math.abs(distanceToTravel.x()) > Math.abs(maxDistanceToTravel.x()))
                maxDistanceToTravel.x = distanceToTravel.x()

            if (Math.abs(distanceToTravel.y()) > Math.abs(maxDistanceToTravel.y()))
                maxDistanceToTravel.y = distanceToTravel.y()

            if (Math.abs(distanceToTravel.z()) > Math.abs(maxDistanceToTravel.z()))
                maxDistanceToTravel.z = distanceToTravel.z()
        }
        return maxDistanceToTravel
    }

    private inline fun addAllSafeAndTranslate(list: ArrayList<Box>, e: Entity, location: Location) {
        e.traits[TraitCollidable::class.java]?.apply {
            if(!collidesWithEntities)
                return@apply

            for(box in collisionBoxes) {
                box.translate(location)
                list.add(box)
            }
        }
    }

    private inline fun <T> addAllSafe(l: ArrayList<T>, e: Array<T>?) {
        if (e != null)
            Collections.addAll(l, *e)
    }
}
