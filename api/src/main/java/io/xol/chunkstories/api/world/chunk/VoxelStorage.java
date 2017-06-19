package io.xol.chunkstories.api.world.chunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface VoxelStorage
{
	/** Used to add additional storage capabilities for special voxels */
	public void load(DataInputStream stream) throws IOException;
	
	/** See load(). */
	public void save(DataOutputStream stream) throws IOException;
}
