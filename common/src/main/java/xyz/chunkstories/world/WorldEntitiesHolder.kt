//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import xyz.chunkstories.api.physics.Box
import org.joml.Vector3d
import org.joml.Vector3dc

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.util.CompoundIterator
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.World.NearEntitiesIterator
import kotlin.math.ceil

class WorldEntitiesHolder(internal val world: World) : Iterable<Entity> {
    internal var backing: MutableMap<Long, Entity> = ConcurrentHashMap()
    internal var backingIterative = ConcurrentLinkedQueue<Entity>()

    // Elements are sorted in increasing hashcode order
    fun insertEntity(entity: Entity) {
        if (backing.put(entity.UUID, entity) == null)
            backingIterative.add(entity)
    }

    fun removeEntity(entity: Entity): Boolean {
        if (backing.remove(entity.UUID) != null)
            backingIterative.remove(entity)
        else
            println(
                    "Warning, EntitiesHolders was asked to remove entity $entity not found in entities list.")

        return false
    }

    fun getEntityByUUID(uuid: Long): Entity? {
        return backing[uuid]
    }

    fun getEntitiesInBox(min: Vector3dc, max: Vector3dc): NearEntitiesIterator {
        val minVoxelX = min.x().toInt()
        val minVoxelY = min.y().toInt()
        val minVoxelZ = min.z().toInt()

        val maxVoxelX = ceil(max.x()).toInt()
        val maxVoxelY = ceil(max.y()).toInt()
        val maxVoxelZ = ceil(max.z()).toInt()

        val box_start_x = sanitizeHorizontalCoordinate(minVoxelX)
        val box_start_y = sanitizeVerticalCoordinate(minVoxelY)
        val box_start_z = sanitizeHorizontalCoordinate(minVoxelZ)

        val box_end_x = sanitizeHorizontalCoordinate(maxVoxelX)
        val box_end_y = sanitizeVerticalCoordinate(maxVoxelY)
        val box_end_z = sanitizeHorizontalCoordinate(maxVoxelZ)

        // Chunk-relative

        val csx = box_start_x shr 5
        val csy = box_start_y shr 5
        val csz = box_start_z shr 5

        val cex = box_end_x shr 5
        val cey = box_end_y shr 5
        val cez = box_end_z shr 5

        val box = Box.fromExtents(box_start_x.toDouble(), box_start_y.toDouble(), box_start_z.toDouble(), (box_end_x - box_start_x).toDouble(),
                (box_end_y - box_start_y).toDouble(), (box_end_z - box_start_z).toDouble())

        // Fast path #1: it's all in one chunk!
        if (csx == cex && csy == cey && csz == cez) {
            val chunk = world.getChunk(csx, csy, csz)
            return if (chunk != null)
                DistanceCheckedIterator(chunk.entitiesWithinChunk.iterator(), box)
            else
                object : NearEntitiesIterator {

                    override fun hasNext(): Boolean {
                        return false
                    }

                    override fun next(): Entity {
                        throw UnsupportedOperationException()
                    }

                    override fun distance(): Double {
                        return -1.0
                    }

                    override fun remove() {
                        throw UnsupportedOperationException()
                    }

                }
        }

        val rsx = csx shr 3
        val rsy = csy shr 3
        val rsz = csz shr 3

        val rex = cex shr 3
        val rey = cey shr 3
        val rez = cez shr 3

        // Fast path #2: all chunks in the same region
        if (rsx == rex && rsy == rey && rsz == rez) {
            val iterators = ArrayList<Iterator<Entity>>()
            for (cx in csx..cex)
                for (cy in csy..cey)
                    for (cz in csz..cez) {
                        // System.out.println(center.x() / 32+":"+center.y() / 32+":"+center.z() / 32);
                        // System.out.println(cx+":"+cy+":"+cz);
                        val chunk = world.getChunk(cx, cy, cz)
                        if (chunk != null)
                            iterators.add(chunk.entitiesWithinChunk.iterator())
                    }

            return DistanceCheckedIterator(CompoundIterator(iterators), box)
        }

        // Slow (and old) path
        return getEntitiesInBoxSlow(min, max)
    }

    internal inner class DistanceCheckedIterator(val i: Iterator<Entity>, val box: Box) : NearEntitiesIterator {
        var next: Entity? = null
        var distance: Double = 0.toDouble()

        init {

            produce()
        }

        override fun hasNext(): Boolean {
            produce()
            return next != null
        }

        private fun produce() {
            while (next == null && i.hasNext()) {
                val candidate = i.next()
                if (box.isPointInside(candidate.location)) {
                    next = candidate
                    distance = candidate.location.distance(box.center)
                }
            }
        }

        override fun next(): Entity? {
            val oldnext = next
            next = null
            produce()
            return oldnext
        }

        override fun distance(): Double {
            return distance
        }


        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    private fun getEntitiesInBoxSlow(min: Vector3dc, max: Vector3dc): NearEntitiesIterator {

        return object : NearEntitiesIterator {

            val minVoxelX = min.x().toInt()
            val minVoxelY = min.y().toInt()
            val minVoxelZ = min.z().toInt()

            val maxVoxelX = ceil(max.x()).toInt()
            val maxVoxelY = ceil(max.y()).toInt()
            val maxVoxelZ = ceil(max.z()).toInt()

            val center = Vector3d(min).add(max).mul(0.5)

            val box_start_x = sanitizeHorizontalCoordinate(minVoxelX)
            val box_start_y = sanitizeVerticalCoordinate(minVoxelY)
            val box_start_z = sanitizeHorizontalCoordinate(minVoxelZ)

            val box_end_x = sanitizeHorizontalCoordinate(maxVoxelX)
            val box_end_y = sanitizeVerticalCoordinate(maxVoxelY)
            val box_end_z = sanitizeHorizontalCoordinate(maxVoxelZ)

            // We currently sort this out by regions, chunks would be more appropriate ?
            val region_start_x = box_start_x / 256
            val region_start_y = box_start_y / 256
            val region_start_z = box_start_z / 256

            val region_end_x = box_end_x / 256
            val region_end_y = box_end_y / 256
            val region_end_z = box_end_z / 256

            var region_x = region_start_x
            var region_y = region_start_y
            var region_z = region_start_z

            var currentRegion = world.getRegion(region_x, region_y, region_z)
            var currentRegionIterator: Iterator<Entity>? = if (currentRegion == null)
                null
            else
                currentRegion!!.entitiesWithinRegion.iterator()
            var next: Entity? = null
            var distance = 0.0

            private fun seekNextEntity() {
                next = null
                while (true) {
                    // Break the loop if we find an entity in the region
                    if (seekNextEntityWithinRegion())
                        break
                    else {
                        // Seek a suitable region if we failed to find anything above
                        if (seekNextRegion())
                            continue
                        else
                            break// Break the loop if we are out of regions to check
                    }
                }
            }

            private fun seekNextEntityWithinRegion(): Boolean {

                if (currentRegionIterator == null)
                    return false
                while (currentRegionIterator!!.hasNext()) {
                    val entity = currentRegionIterator!!.next()
                    // Check if it's inside the box for realz

                    val loc = entity.location

                    val locx = loc.x().toInt()
                    // Normal case, check if it's in the bounds, wrap-arround case, check if it's
                    // outside
                    if (box_start_x > box_end_x == (locx >= box_start_x && locx <= box_end_x))
                        continue

                    val locy = loc.y().toInt()
                    // Normal case, check if it's in the bounds, wrap-arround case, check if it's
                    // outside
                    if (box_start_y > box_end_y == (locy >= box_start_y && locy <= box_end_y))
                        continue

                    val locz = loc.z().toInt()
                    // Normal case, check if it's in the bounds, wrap-arround case, check if it's
                    // outside
                    if (box_start_z > box_end_z == (locz >= box_start_z && locz <= box_end_z))
                        continue

                    // if(Math.abs(check.getX()) <= boxSize.getX() && Math.abs(check.getY()) <=
                    // boxSize.getY() && Math.abs(check.getZ()) <= boxSize.getZ())
                    run {
                        // Found a good one
                        this.next = entity

                        this.distance = entity.location.distance(center)
                        return true
                    }
                }

                // We found nothing :(
                currentRegionIterator = null
                return false
            }

            private fun seekNextRegion(): Boolean {
                currentRegion = null
                while (true) {
                    // Found one !
                    if (currentRegion != null) {
                        currentRegionIterator = currentRegion!!.entitiesWithinRegion.iterator()
                        return true
                    }

                    region_x++
                    // Wrap arround in X dimension to Y
                    if (region_x > region_end_x) {
                        region_x = 0
                        region_y++
                    }
                    // Then Y to Z
                    if (region_y > region_end_y) {
                        region_y = 0
                        region_z++
                    }
                    // We are done here
                    if (region_z > region_end_z)
                        return false

                    currentRegion = world.getRegion(region_x, region_y, region_z)
                }
            }

            override fun hasNext(): Boolean {
                if (next == null)
                    seekNextEntity()
                return next != null
            }

            override fun next(): Entity? {
                val entity = next
                seekNextEntity()
                return entity
            }

            override fun distance(): Double {
                return distance
            }


            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }

    private fun sanitizeHorizontalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        coordinate = coordinate % (world.sizeInChunks * 32)
        if (coordinate < 0)
            coordinate += world.sizeInChunks * 32
        return coordinate
    }

    private fun sanitizeVerticalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        if (coordinate < 0)
            coordinate = 0
        if (coordinate >= world.worldInfo.size.heightInChunks * 32)
            coordinate = world.worldInfo.size.heightInChunks * 32 - 1
        return coordinate
    }

    override fun iterator(): Iterator<Entity> {
        return backingIterative.iterator()
    }
}
