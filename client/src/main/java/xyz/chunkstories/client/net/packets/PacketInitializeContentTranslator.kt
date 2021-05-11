package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.net.packets.PacketContentTranslator
import java.io.*

class PacketInitializeContentTranslator(engine: Engine) : PacketContentTranslator(engine) {

    @Throws(IOException::class, PacketProcessingException::class)
    override fun receive(dis: DataInputStream, user: UserConnection?) {
        /*val serializedText = dis.readUTF()

        val bais = ByteArrayInputStream(serializedText.toByteArray(charset("UTF-8")))
        val reader = BufferedReader(InputStreamReader(bais, "UTF-8"))
        try {
            val translator = LoadedContentTranslator((context as ClientPacketsEncoderDecoder).client.content, reader)
            val cCommon = context as PacketsEncoderDecoder
            cCommon.contentTranslator = translator
            context.logger().info("Successfully installed content translator")
            cCommon.connection.handleSystemRequest("world/translator_ok")

        } catch (e: IncompatibleContentException) {
            e.printStackTrace()
        }

        reader.close()*/
        TODO()
    }
}