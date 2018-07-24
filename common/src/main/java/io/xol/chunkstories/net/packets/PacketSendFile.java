//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;

public class PacketSendFile extends Packet {
	public String fileTag;
	public File file;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeUTF(fileTag);

		if (file.exists()) {
			out.writeLong(file.length());
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int read;
			while (true) {
				read = fis.read(buffer);
				// System.out.println("read"+read);
				if (read > 0)
					out.write(buffer, 0, read);
				else
					break;
			}
			fis.close();
		} else
			out.writeLong(0L);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor)
			throws IOException, PacketProcessingException {
		// Ignore packets incomming
	}
}
