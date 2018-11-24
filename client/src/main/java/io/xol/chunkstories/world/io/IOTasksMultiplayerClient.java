//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.client.ingame.IngameClientImplementation;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.net.packets.PacketChunkCompressedData;
import io.xol.chunkstories.net.packets.PacketHeightmap;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;
import io.xol.chunkstories.world.storage.RegionImplementation;

public class IOTasksMultiplayerClient extends IOTasks {
	IngameClientImplementation client;
	ServerConnection connection;

	public IOTasksMultiplayerClient(WorldClientRemote world) {
		super(world);
		this.connection = world.getConnection();
		this.client = world.getClient();

		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	MessageDigest md = null;

	static ThreadLocal<byte[]> unCompressedSummariesData = new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			// Buffer for summaries
			return new byte[256 * 256 * 4 * 2];
		}
	};

	public class IOTaskProcessCompressedHeightmapArrival extends IOTask {
		PacketHeightmap packet;

		public IOTaskProcessCompressedHeightmapArrival(PacketHeightmap packet) {
			this.packet = packet;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor) {
			HeightmapImplementation heightmap = world.getRegionsSummariesHolder().getHeightmapWorldCoordinates(packet.rx * 256, packet.rz * 256);
			if (heightmap == null) {
				logger().error("Summary data arrived for " + packet.rx + ": " + packet.rz
						+ "but there was no region summary waiting for it ?");
				return true;
			}

			int[] heights = new int[256 * 256];
			int[] ids = new int[256 * 256];

			byte[] unCompressedSummaries = unCompressedSummariesData.get();
			unCompressedSummaries = HeightmapImplementation.Companion.getDecompressor().decompress(packet.compressedData,
					256 * 256 * 4 * 2);
			IntBuffer ib = ByteBuffer.wrap(unCompressedSummaries).asIntBuffer();
			ib.get(heights, 0, 256 * 256);
			ib.get(ids, 0, 256 * 256);

			heightmap.whenDataLoadedCallback(heights, ids);

			return true;
		}

		/*
		 * @Override public boolean equals(Object o) { //All packets are unique return
		 * false; }
		 */

		@Override
		public int hashCode() {
			return 8792;
		}
	}

	public void requestHeightmapProcess(PacketHeightmap packet) {
		IOTaskProcessCompressedHeightmapArrival task = new IOTaskProcessCompressedHeightmapArrival(packet);
		scheduleTask(task);
	}

	public void handlePacketWorldStreaming(PacketWorldStreaming packet) throws IllegalPacketException {

		// Region summaries
		if (packet instanceof PacketHeightmap) {
			this.requestHeightmapProcess((PacketHeightmap) packet);
			// Chunk data
		} else if (packet instanceof PacketChunkCompressedData) {
			RegionImplementation region = world.getRegionChunkCoordinates(((PacketChunkCompressedData) packet).x,
					((PacketChunkCompressedData) packet).y, ((PacketChunkCompressedData) packet).z);

			// This *can* happen, ie if the player flies fucking fast and the server sends
			// the chunk but he's already fucking gone
			if (region == null)
				return;
			region.getChunkHolder(((PacketChunkCompressedData) packet).x, ((PacketChunkCompressedData) packet).y,
					((PacketChunkCompressedData) packet).z).eventLoadFinishes(((PacketChunkCompressedData) packet).data);
		}

		// Else
		else
			throw new IllegalPacketException(packet) {
				private static final long serialVersionUID = 7843266994553911002L;

				@Override
				public String getMessage() {
					return "Illegal packet received : This type of World streaming packet isn't recognized ( "
							+ packet.getClass().getName() + " )";
				}
			};
	}
}
