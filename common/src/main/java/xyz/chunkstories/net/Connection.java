//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.packets.PacketText;

public abstract class Connection {
	protected final String remoteAddress;
	protected final int port;

	protected static final Logger logger = LoggerFactory.getLogger("net.connection");

	public Connection(String remoteAddress, int port) {
		super();
		this.remoteAddress = remoteAddress;
		this.port = port;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public int getPort() {
		return port;
	}

	public abstract boolean connect();

	public abstract void flush();

	public abstract void handleDatagram(LogicalPacketDatagram datagram)
			throws IOException, PacketProcessingException, IllegalPacketException;

	public abstract boolean handleSystemRequest(String msg);

	public void sendTextMessage(String string) {
		PacketText packet = new PacketText();
		packet.text = string;
		pushPacket(packet);
	}

	public abstract void pushPacket(Packet packet);

	public abstract PacketsEncoderDecoder getEncoderDecoder();

	public abstract boolean close();

	public abstract boolean isOpen();

	// Below is stuff for downloading/uploading of files

	Map<String, PendingDownload> fileStreamingRequests = new ConcurrentHashMap<>();

	/**
	 * Hints the connection logic that a file with a certain tag is to be expected,
	 * and provides it with a location to save it. Unexpected file streaming will be
	 * discarded.
	 */
	public void registerExpectedFileStreaming(String fileTag, File whereToSave, DownloadStartAction action) {
		if (fileStreamingRequests.putIfAbsent(fileTag, new PendingDownload(whereToSave, action)) == null) {

		} else {
			logger.warn("Requesting twice file: " + fileTag);
		}
	}

	public void registerExpectedFileStreaming(String fileTag, File whereToSave) {
		registerExpectedFileStreaming(fileTag, whereToSave, null);
	}

	/** Retreives and removes the expected location for the specified fileTag */
	public PendingDownload getLocationForExpectedFile(String fileTag) {
		return fileStreamingRequests.remove(fileTag);
	}

	public interface DownloadStatus {

		public int bytesDownloaded();

		public int totalBytes();

		/** Waits for the download to end and returns true if successful */
		public boolean waitForEnd();
	}

	public interface DownloadStartAction {
		public void onStart(DownloadStatus status);
	}

	public class PendingDownload {
		public final File f;
		public final DownloadStartAction a;

		public PendingDownload(File f, DownloadStartAction a) {
			super();
			this.f = f;
			this.a = a;
		}
	}
}
