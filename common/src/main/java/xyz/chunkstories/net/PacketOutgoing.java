//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.DataOutputStream;
import java.io.IOException;

public interface PacketOutgoing {
	public void write(DataOutputStream out) throws IOException;
}
