package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.net.PacketReceptionContext
import xyz.chunkstories.api.net.PacketSender
import xyz.chunkstories.client.net.ClientPacketsEncoderDecoder
import xyz.chunkstories.content.translator.IncompatibleContentException
import xyz.chunkstories.content.translator.LoadedContentTranslator
import xyz.chunkstories.net.PacketsEncoderDecoder
import xyz.chunkstories.net.packets.PacketContentTranslator
import java.io.*

class PacketInitializeContentTranslator : PacketContentTranslator() {
    @Throws(IOException::class, PacketProcessingException::class)
    override fun process(sender: PacketSender, dis: DataInputStream, context: PacketReceptionContext) {
        val serializedText = dis.readUTF()

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

        reader.close()

    }
}