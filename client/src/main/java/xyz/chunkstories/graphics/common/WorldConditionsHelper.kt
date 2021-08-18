package xyz.chunkstories.graphics.common

import org.joml.Vector3d
import xyz.chunkstories.api.graphics.structs.WorldConditions
import xyz.chunkstories.api.world.World

fun World.getConditions(): WorldConditions {
    val dayCycle = sky.timeOfDay
    val sunPos = Vector3d(0.5, -1.0, 0.0)
    sunPos.rotateAbout(dayCycle * Math.PI * 2.0, 1.0, 0.0, 0.0).normalize()
    return WorldConditions(
            time = dayCycle,
            sunPosition = sunPos,
            cloudyness = sky.overcast
    )
}