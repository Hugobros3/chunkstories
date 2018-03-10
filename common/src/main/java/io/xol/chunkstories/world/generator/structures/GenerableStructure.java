//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.generator.structures;

import io.xol.chunkstories.api.world.chunk.Chunk;



public abstract class GenerableStructure
{
	// Trees, houses and small dungeons will be generated with this class
	// it basically is a container for a drawing method that has to apply to
	// multiple chunks
	int oriX, oriY, oriZ;

	public GenerableStructure(int x, int y, int z)
	{
		oriX = x;
		oriY = y;
		oriZ = z;
	}

	public abstract void draw(Chunk c);
	// Draws as much as it can iside the chunk

}
