//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net

import java.io.DataOutputStream
import java.io.IOException

interface PacketOutgoing {
    fun write(out: DataOutputStream)
}