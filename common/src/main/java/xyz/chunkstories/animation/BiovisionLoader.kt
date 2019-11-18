package xyz.chunkstories.animation

import org.joml.Vector3f
import org.joml.Vector3fc
import java.util.*

fun loadBiviosionFile(text: String): BiovisionAnimation {
    val tokensStream = LinkedList(text.replace("\n", " ").replace("\t", "").split(" "))
    tokensStream.removeAll { it.isEmpty() }

    tokensStream.expect("HIERARCHY")
    tokensStream.expect("ROOT")

    val allBones = mutableMapOf<String, BiovisionBone>()
    var offsetInDataArray = 0

    fun readBone(boneName: String): BiovisionBone {
        tokensStream.expect("{")
        tokensStream.expect("OFFSET")
        val offset = tokensStream.readVec3()
        tokensStream.expect("CHANNELS")
        val channels = tokensStream.readInt()
        when (channels) {
            3 -> {
                tokensStream.expect("Xrotation")
                tokensStream.expect("Yrotation")
                tokensStream.expect("Zrotation")
            }
            6 -> {
                tokensStream.expect("Xposition")
                tokensStream.expect("Yposition")
                tokensStream.expect("Zposition")

                tokensStream.expect("Xrotation")
                tokensStream.expect("Yrotation")
                tokensStream.expect("Zrotation")
            }
            else -> throw Exception("Channels must be either 3 or 6")
        }

        val offsetForThisOne = offsetInDataArray
        offsetInDataArray += channels

        val children = mutableListOf<BiovisionBone>()
        var next = tokensStream.readToken()
        while (true) {
            if (when (next) {
                        "}" -> true
                        "JOINT" -> {
                            val childrenBoneName = tokensStream.readToken()
                            val bone = readBone(childrenBoneName)
                            children.add(bone)
                            false
                        }
                        "End" -> {
                            tokensStream.skipUntil("}")
                            false
                        }
                        else -> false
                    }) break
            next = tokensStream.readToken()
        }

        val bone = BiovisionBone(boneName, channels, offsetForThisOne, offset, children)
        allBones[boneName] = bone
        for (child in children) {
            child.parent = bone
        }

        return bone
    }

    val rootBoneName = tokensStream.readToken()
    val rootBone = readBone(rootBoneName)

    tokensStream.expect("MOTION")
    tokensStream.expect("Frames:")
    val frames = tokensStream.readInt()
    tokensStream.expect("Frame")
    tokensStream.expect("Time:")
    val frameTime = tokensStream.readFloat()

    val animationData = Array(frames) { FloatArray(offsetInDataArray) { tokensStream.readFloat() } }
    val animation = BiovisionAnimation(frames, frameTime, rootBone, allBones, animationData)

    for(bone in allBones.values) {
        bone.animation = animation
    }

    return animation
}

fun MutableList<String>.readToken() = this.removeAt(0)
fun MutableList<String>.readInt(): Int = readToken().toInt()
fun MutableList<String>.readFloat(): Float = readToken().toFloat()
fun MutableList<String>.readVec3(): Vector3fc = Vector3f(readFloat(), readFloat(), readFloat())

fun MutableList<String>.expect(token: String) = expect(token, "Expected token '$token'")

fun MutableList<String>.expect(token: String, errorMessage: String) {
    if (this[0] == token) {
        this.removeAt(0)
    } else {
        throw Exception(errorMessage)
    }
}

fun MutableList<String>.skipUntil(token: String) {
    while (true) {
        val t = readToken()
        if (t == token) break
    }
}