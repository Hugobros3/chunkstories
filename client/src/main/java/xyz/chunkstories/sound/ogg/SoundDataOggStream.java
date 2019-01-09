//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound.ogg;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGetError;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import xyz.chunkstories.sound.SoundDataBuffered;

public class SoundDataOggStream extends SoundDataBuffered {
	private int openAlBufferId = -1;

	private static int BUFFER_SIZE = 22050 * 4; // buffers size
	private byte[] scratch = new byte[BUFFER_SIZE];
	private ByteBuffer buffer;
	private OggInputStream oggInput;

	private int format;

	private long length = -1;
	public String name = "undefined ta mère la globachienasse galactique";

	public SoundDataOggStream(InputStream is) {
		try {
			oggInput = new OggInputStream(new DataInputStream(is));
			format = oggInput.getChannel() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;

			buffer = MemoryUtil.memAlloc(BUFFER_SIZE);// ByteBuffer.allocateDirect(BUFFER_SIZE);
			length = 0; // Empty size until we request some
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fillBuffer(int alId) throws IOException {
		int remaining = BUFFER_SIZE;
		while (remaining > 0) {
			int read = oggInput.read(scratch, BUFFER_SIZE - remaining, remaining);
			if (read < 0) {
				break;
			}
			remaining -= read;
		}
		if (BUFFER_SIZE - remaining == 0)
			length = -1;
		length += (BUFFER_SIZE - remaining) * 2 * 1000 / (oggInput.getChannel() * oggInput.getRate());
		buffer.clear();
		buffer.put(scratch);
		buffer.flip();

		int result;
		if ((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error b4  filling buffer : " + SoundDataOggSample.getALErrorString(result));

		alBufferData(alId, format, buffer, oggInput.getRate());

		if ((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error after filling buffer : " + SoundDataOggSample.getALErrorString(result));
	}

	@Override
	public int getBuffer() {
		return openAlBufferId;
	}

	@Override
	public void destroy() {
		if (buffer != null)
			MemoryUtil.memFree(buffer);
		// System.out.println("destroy command issued");

		int result;
		if ((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error at removal :" + SoundDataOggSample.getALErrorString(result));
	}

	@Override
	public int uploadNextPage(int alId) {
		try {
			int nid = alGenBuffers();
			fillBuffer(nid);
			return nid;
		} catch (Exception e) {
			length = -1;
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public long getLengthMs() {
		return length;
	}

	@Override
	public boolean loadedOk() {
		return length != -1;
	}

	@Override
	public String getName() {
		return name;
	}
}
