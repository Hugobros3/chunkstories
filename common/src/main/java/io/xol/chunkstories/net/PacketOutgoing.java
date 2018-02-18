package io.xol.chunkstories.net;

import java.io.DataOutputStream;
import java.io.IOException;

public interface PacketOutgoing {
	public void write(DataOutputStream out) throws IOException;
}
