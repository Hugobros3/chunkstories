//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.io;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.net.PacketWorldStreaming;
import xyz.chunkstories.api.workers.TaskExecutor;
import xyz.chunkstories.client.ingame.IngameClientImplementation;
import xyz.chunkstories.client.net.ServerConnection;
import xyz.chunkstories.net.packets.PacketChunkCompressedData;
import xyz.chunkstories.net.packets.PacketHeightmap;
import xyz.chunkstories.world.WorldClientRemote;
import xyz.chunkstories.world.chunk.ChunkCompressedData;
import xyz.chunkstories.world.chunk.ChunkImplementation;
import xyz.chunkstories.world.heightmap.HeightmapImplementation;
import xyz.chunkstories.world.chunk.ChunkHolderImplementation;
import xyz.chunkstories.world.region.RegionImplementation;

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
			HeightmapImplementation heightmap = world.getHeightmapsManager().getHeightmapWorldCoordinates(packet.rx * 256, packet.rz * 256);
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

			heightmap.eventLoadingFinished(heights, ids);

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
			RegionImplementation region = world.getRegionsManager().getRegionChunkCoordinates(((PacketChunkCompressedData) packet).getX(),
					((PacketChunkCompressedData) packet).getY(), ((PacketChunkCompressedData) packet).getZ());

			// This *can* happen, ie if the player flies too fast and by the time he receives the chunk data the region has already been gc'ed
			if (region == null)
				return;

			ChunkCompressedData compressedData = ((PacketChunkCompressedData) packet).getData();
			ChunkHolderImplementation chunkHolder = region.getChunkHolder(((PacketChunkCompressedData) packet).getX(),
					((PacketChunkCompressedData) packet).getY(), ((PacketChunkCompressedData) packet).getZ());
			ChunkImplementation chunk = new ChunkImplementation(chunkHolder, chunkHolder.getChunkX(), chunkHolder.getChunkY(), chunkHolder.getChunkZ(), compressedData);
			chunkHolder.eventLoadFinishes(chunk);
		}

		// Else
		else
			throw new IllegalPacketException(packet);/* {
				private static final long serialVersionUID = 7843266994553911002L;

				@Override
				public String getMessage() {
					return "Illegal packet received : This type of World streaming packet isn't recognized ( "
							+ packet.getClass().getName() + " )";
				}
			};*/
	}
}
