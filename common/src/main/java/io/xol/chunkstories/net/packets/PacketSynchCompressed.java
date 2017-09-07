package io.xol.chunkstories.net.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/** A simple way to accelerate packet delivery, add-up synch packets in one large buffer rather than multiple small ones */
public class PacketSynchCompressed extends Packet{
	ThreadLocal<CompressionBuffer> threadLocal = new ThreadLocal<CompressionBuffer>() {

		@Override
		protected CompressionBuffer initialValue() {
			return new CompressionBuffer();
		}
		
	};
	
	class CompressionBuffer {
		LZ4Factory factory = LZ4Factory.fastestInstance();
		LZ4FastDecompressor decompressor = factory.fastDecompressor();
		LZ4Compressor compressor = factory.fastCompressor();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[0x100000];
		byte[] buffer2 = new byte[0x100000];
		ByteArrayInputStream bais = null;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException {
		
		CompressionBuffer cbuf = threadLocal.get();
		
		int compressedLength = cbuf.compressor.compress(cbuf.buffer, cbuf.buffer2);
		out.writeInt(compressedLength);
		out.write(cbuf.buffer2);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor)
			throws IOException, PacketProcessingException {

		CompressionBuffer cbuf = threadLocal.get();
		
		int compressedLength = in.readInt();
		
	}
}
