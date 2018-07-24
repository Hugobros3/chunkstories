//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.content.translator.AbstractContentTranslator;
import io.xol.chunkstories.content.translator.IncompatibleContentException;
import io.xol.chunkstories.content.translator.LoadedContentTranslator;
import io.xol.chunkstories.net.PacketsContextCommon;

public class PacketContentTranslator extends Packet {

	public PacketContentTranslator() {

	}

	private String serializedText;

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
		this.serializedText = in.readUTF();

		ByteArrayInputStream bais = new ByteArrayInputStream(serializedText.getBytes("UTF-8"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
		try {
			OnlineContentTranslator translator = new LoadedContentTranslator(context.getContext().getContent(), reader);
			PacketsContextCommon cCommon = (PacketsContextCommon) context;
			cCommon.setContentTranslator(translator);
			context.logger().info("Successfully installed content translator");
			cCommon.getConnection().handleSystemRequest("world/translator_ok");

		} catch (IncompatibleContentException e) {
			e.printStackTrace();
		}
		reader.close();

	}

}
