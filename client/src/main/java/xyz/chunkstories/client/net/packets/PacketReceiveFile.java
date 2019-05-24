//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSender;
import xyz.chunkstories.client.net.ClientPacketsEncoderDecoder;
import xyz.chunkstories.net.Connection.DownloadStatus;
import xyz.chunkstories.net.Connection.PendingDownload;
import xyz.chunkstories.net.PacketsEncoderDecoder;
import xyz.chunkstories.net.packets.PacketSendFile;

public class PacketReceiveFile extends PacketSendFile {
	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor)
			throws IOException, PacketProcessingException {
		if (!(processor instanceof ClientPacketsEncoderDecoder))
			return;

		// ClientPacketsEncoderDecoder cppi = (ClientPacketsEncoderDecoder)processor;

		String fileTag = in.readUTF();
		long fileLength = in.readLong();

		if (fileLength > 0) {
			PacketsEncoderDecoder context = (PacketsEncoderDecoder) processor;
			PendingDownload pendingDownload = context.getConnection().getLocationForExpectedFile(fileTag);
			if (pendingDownload == null)
				throw new IOException("Received unexpected PacketFile with tag: " + fileTag);

			// Class to report back the download status to whoever requested it via the
			// onStart callback
			PacketFileDownloadStatus status = new PacketFileDownloadStatus((int) fileLength);
			if (pendingDownload.getA() != null)
				pendingDownload.getA().invoke(status);

			long remaining = fileLength;
			FileOutputStream fos = new FileOutputStream(pendingDownload.getF());
			byte[] buffer = new byte[4096];
			while (remaining > 0) {
				long toRead = Math.min(4096, remaining);
				System.out.println("Working ! ...");
				// cppi.getConnection().getCurrentlyDownloadedFileProgress().setStepText("Downloading
				// "+fileTag+", "+(fileLength - remaining)/1024+"/"+fileLength/1024+"kb");
				int actuallyRead = in.read(buffer, 0, (int) toRead);
				fos.write(buffer, 0, (int) actuallyRead);
				remaining -= actuallyRead;
				status.downloaded += actuallyRead;
			}

			status.end.release();
			fos.close();
		}
	}

	class PacketFileDownloadStatus implements DownloadStatus {

		public PacketFileDownloadStatus(int fileLength) {
			this.fileLength = fileLength;
		}

		final int fileLength;
		int downloaded;
		Semaphore end = new Semaphore(0);

		@Override
		public int bytesDownloaded() {
			return downloaded;
		}

		@Override
		public int totalBytes() {
			return (int) fileLength;
		}

		@Override
		public boolean waitsUntilDone() {
			end.acquireUninterruptibly();
			return true;
		}
	}

}
