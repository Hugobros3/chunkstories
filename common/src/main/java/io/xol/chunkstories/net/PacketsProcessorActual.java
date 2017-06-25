package io.xol.chunkstories.net;

import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.net.PacketsProcessorCommon.PendingSynchPacket;

public interface PacketsProcessorActual extends PacketsProcessor {

	public PendingSynchPacket getPendingSynchPacket();
}
