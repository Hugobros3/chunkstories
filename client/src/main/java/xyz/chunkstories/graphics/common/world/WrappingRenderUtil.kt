package xyz.chunkstories.graphics.common.world

import xyz.chunkstories.api.world.World

/**
 * Divide the world into sections, in each axis
 * Section -1 is from 0 to 1/4th the width of the world
 * Section 1 is from 3/4th the width of the world to the end
 * Section 0 is the middle
 *
 * By diffing the sections of the camera & an object, we know if we have to offset the object, and by how much
 */

fun section(position: Double, world: World): Int {
    val worldSize = world.properties.size.sizeInChunks * 32.0
    val leftSection = worldSize / 4
    val rightSection = worldSize - leftSection
    return if (position <= leftSection) -1 else if(position >= rightSection) 1 else 0
}

fun sectionChunk(position: Int, world: World): Int {
    val worldSize = world.properties.size.sizeInChunks
    val leftSection = worldSize / 4
    val rightSection = worldSize - leftSection
    return if (position <= leftSection) -1 else if(position >= rightSection) 1 else 0
}

fun sectionRegion(position: Int, world: World): Int {
    val worldSize = world.properties.size.sizeInChunks / 8
    val leftSection = worldSize / 4
    val rightSection = worldSize - leftSection
    return if (position <= leftSection) -1 else if(position >= rightSection) 1 else 0
}

fun shouldWrap(cameraSection: Int, objectSection: Int): Int {
    val diff = cameraSection - objectSection
    // camera looking at an object all the way to the right
    if(diff == -2)
        return -1
    // camera looking at an object all the way to the left
    else if(diff == 2) {
        return 1
    }
    return 0
}