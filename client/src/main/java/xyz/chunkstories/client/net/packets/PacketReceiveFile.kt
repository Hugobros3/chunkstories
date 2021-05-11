//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.Engine
import xyz.chunkstories.net.packets.PacketSendFile
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.UserConnection
import java.lang.RuntimeException
import xyz.chunkstories.net.Connection.DownloadStatus
import java.io.DataInputStream
import java.util.concurrent.Semaphore

class PacketReceiveFile(engine: Engine) : PacketSendFile(engine) {
    override fun receive(dis: DataInputStream, user: UserConnection?) {
        /*String fileTag = dis.readUTF();
		long fileLength = dis.readLong();

		if (fileLength > 0) {
			// PacketsEncoderDecoder context = (PacketsEncoderDecoder) processor;
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
				// cppi.getConnection().getCurrentlyDownloadedFileProgress().setStepText("Downloading
				// "+fileTag+", "+(fileLength - remaining)/1024+"/"+fileLength/1024+"kb");
				int actuallyRead = in.read(buffer, 0, (int) toRead);
				fos.write(buffer, 0, (int) actuallyRead);
				remaining -= actuallyRead;
				status.downloaded += actuallyRead;
			}

			status.end.release();
			fos.close();
		}*/
        TODO()
    }

    internal class PacketFileDownloadStatus(val fileLength: Int) : DownloadStatus {
        var downloaded = 0
        var end = Semaphore(0)
        override fun bytesDownloaded(): Int {
            return downloaded
        }

        override fun totalBytes(): Int {
            return fileLength
        }

        override fun waitsUntilDone(): Boolean {
            end.acquireUninterruptibly()
            return true
        }
    }
}