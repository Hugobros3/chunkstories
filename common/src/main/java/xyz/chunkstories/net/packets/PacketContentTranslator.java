//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.net.*;
import xyz.chunkstories.content.translator.AbstractContentTranslator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketContentTranslator extends Packet {

    public PacketContentTranslator() {

    }

    protected String serializedText;

    public PacketContentTranslator(AbstractContentTranslator sendme) {
        this.serializedText = sendme.toString(true);
    }

    @Override
    public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext context)
            throws IOException {
        out.writeUTF(serializedText);
    }

    @Override
    public void process(PacketSender sender, DataInputStream in, PacketReceptionContext context)
            throws IOException, PacketProcessingException {
        throw new UnsupportedOperationException();
    }

}
