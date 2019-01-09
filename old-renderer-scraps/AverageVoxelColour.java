//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.scrap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import xyz.chunkstories.api.rendering.textures.Texture1D;
import xyz.chunkstories.api.rendering.textures.TextureFormat;
import xyz.chunkstories.api.voxel.textures.VoxelTexture;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.renderer.opengl.texture.Texture1DGL;
import xyz.chunkstories.voxel.VoxelTextureAtlased;

public class AverageVoxelColour {

	private Texture1DGL blockTexturesSummary;

	public AverageVoxelColour(World world) {
		blockTexturesSummary = new Texture1DGL(TextureFormat.RGBA_8BPP);

		int size = 512;
		ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		int counter = 0;
		Iterator<VoxelTexture> i = world.getContent().voxels().textures().all();
		while (i.hasNext() && counter < size) {
			VoxelTextureAtlased voxelTexture = (VoxelTextureAtlased) i.next();

			bb.put((byte) (voxelTexture.getColor().x() * 255));
			bb.put((byte) (voxelTexture.getColor().y() * 255));
			bb.put((byte) (voxelTexture.getColor().z() * 255));
			bb.put((byte) (voxelTexture.getColor().w() * 255));

			voxelTexture.positionInColorIndex = counter;
			counter++;
		}

		// Padding
		while (counter < size) {
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			bb.put((byte) (0));
			counter++;
		}

		bb.flip();

		blockTexturesSummary.uploadTextureData(size, bb);
		blockTexturesSummary.setLinearFiltering(false);
	}

	public Texture1D get() {
		return blockTexturesSummary;
	}
}
