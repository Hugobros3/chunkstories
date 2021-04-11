//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.player.Player
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream

open class PacketSendFile : Packet() {
    lateinit var fileTag: String
    lateinit var file: File

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(fileTag)
        if (file.exists()) {
            dos.writeLong(file.length())
            val fis = FileInputStream(file)
            val buffer = ByteArray(4096)
            var read: Int
            while (true) {
                read = fis.read(buffer)
                if (read > 0) dos.write(buffer, 0, read) else break
            }
            fis.close()
        } else dos.writeLong(0L)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        throw Exception("Unexpected")
    }
}