//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;

public class PacketHeightmap extends PacketWorldStreaming {
	// Server-side
	public HeightmapImplementation summary;

	// Client-side
	public int rx, rz;
	public byte[] compressedData;

	public PacketHeightmap(World world) {
		super(world);
	}

	public PacketHeightmap(HeightmapImplementation summary) {
		super(summary.getWorld());
		this.summary = summary;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException {
		out.writeInt(summary.getRegionX());
		out.writeInt(summary.getRegionZ());

		int[] heights = summary.getHeightData();
		int[] ids = summary.getVoxelData();

		ByteBuffer compressMe = ByteBuffer.allocateDirect(256 * 256 * 4 * 2);
		for (int i = 0; i < 256 * 256; i++)
			compressMe.putInt(heights[i]);
		for (int i = 0; i < 256 * 256; i++)
			compressMe.putInt(ids[i]);

		compressMe.flip();
		byte[] unCompressed = new byte[compressMe.remaining()];
		compressMe.get(unCompressed);
		byte[] compressedData = HeightmapImplementation.Companion.getCompressor().compress(unCompressed);
		out.writeInt(compressedData.length);
		out.write(compressedData);
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		rx = in.readInt();
		rz = in.readInt();
		int dataLength = in.readInt();
		compressedData = new byte[dataLength];
		in.readFully(compressedData);

		// No fancy processing here, these packets are handled automatically by the IO
		// controller on the client due to their
		// PacketWorldStreaming nature
	}
}
