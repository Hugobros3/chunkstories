//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class CompressedData {
	public final byte[] voxelCompressedData;
	public final byte[] voxelComponentsCompressedData;
	public final byte[] entitiesCompressedData;

	protected LZ4Factory factory = LZ4Factory.fastestInstance();

	ThreadLocal<LZ4FastDecompressor> localDecompressor = new ThreadLocal<LZ4FastDecompressor>() {
		@Override
		protected LZ4FastDecompressor initialValue() {
			return factory.fastDecompressor();
		}
	};

	public CompressedData(byte[] voxelCompressedData, byte[] voxelComponentsCompressedData,
			byte[] entitiesCompressedData) {
		this.voxelCompressedData = voxelCompressedData;
		this.voxelComponentsCompressedData = voxelComponentsCompressedData;
		this.entitiesCompressedData = entitiesCompressedData;
	}

	public int[] getVoxelData() throws UnloadableChunkDataException {
		if (voxelCompressedData == null)
			return null;

		ByteBuffer f4st = MemoryUtil.memAlloc(voxelCompressedData.length);
		f4st.put(voxelCompressedData);
		f4st.flip();

		// System.out.println(f4st);

		ByteBuffer t3mp = MemoryUtil.memAlloc(32 * 32 * 32 * 4);
		try {
			localDecompressor.get().decompress(f4st, t3mp);

			// System.out.println(t3mp);

			t3mp.flip();

			MemoryUtil.memFree(f4st);

			int data[] = new int[32 * 32 * 32];
			t3mp.asIntBuffer().get(data);

			MemoryUtil.memFree(t3mp);

			return data;
		} catch (LZ4Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			MemoryUtil.memFree(f4st);
			MemoryUtil.memFree(t3mp);
			throw new UnloadableChunkDataException("LZ4 decompression failed.");
		}
	}

	public int getTotalCompressedSize() {
		int headers_size = 4 + 4 + 4; // 3 sections, 3 ints of 4 bytes
		int voxel_data_size = voxelCompressedData == null ? 0 : voxelCompressedData.length;
		int voxel_components_size = voxelComponentsCompressedData == null ? 0 : voxelComponentsCompressedData.length;
		int entities_size = entitiesCompressedData == null ? 0 : entitiesCompressedData.length;

		return headers_size + voxel_data_size + voxel_components_size + entities_size;
	}
}