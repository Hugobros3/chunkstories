//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound.ogg;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_INVALID_ENUM;
import static org.lwjgl.openal.AL10.AL_INVALID_NAME;
import static org.lwjgl.openal.AL10.AL_INVALID_OPERATION;
import static org.lwjgl.openal.AL10.AL_INVALID_VALUE;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.AL_OUT_OF_MEMORY;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGetError;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.sound.SoundData;

public class SoundDataOggSample extends SoundData {
	private int openAlBufferId = -1;

	private long length = -1;
	public String name = "undefined";

	public SoundDataOggSample(Asset asset) {
		if (asset != null) {
			try {
				ByteArrayOutputStream sampleDataOutputStream = new ByteArrayOutputStream();
				OggInputStream oggInput = new OggInputStream(new DataInputStream(asset.read()));
				while (!oggInput.atEnd()) {
					sampleDataOutputStream.write(oggInput.read());
				}
				oggInput.close();

				byte[] sampleDataByteArray = sampleDataOutputStream.toByteArray();

				ByteBuffer sampleDataByteBuffer = BufferUtils.createByteBuffer(sampleDataByteArray.length);
				sampleDataByteBuffer.put(sampleDataByteArray);
				sampleDataByteBuffer.flip();

				// Compute length in milliseconds based on sample length divided by rate & channels
				length = sampleDataByteArray.length * 1000L / (oggInput.info.channels * oggInput.getRate());

				int format = oggInput.info.channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;

				openAlBufferId = alGenBuffers();
				alBufferData(openAlBufferId, format, sampleDataByteBuffer, oggInput.getRate());

				int result;
				if ((result = alGetError()) != AL_NO_ERROR)
					System.out.println(getALErrorString(result));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static String getALErrorString(int err) {
		switch (err) {
		case AL_NO_ERROR:
			return "AL_NO_ERROR";
		case AL_INVALID_NAME:
			return "AL_INVALID_NAME";
		case AL_INVALID_ENUM:
			return "AL_INVALID_ENUM";
		case AL_INVALID_VALUE:
			return "AL_INVALID_VALUE";
		case AL_INVALID_OPERATION:
			return "AL_INVALID_OPERATION";
		case AL_OUT_OF_MEMORY:
			return "AL_OUT_OF_MEMORY";
		default:
			return "No such error code";
		}
	}

	@Override
	public int getBuffer() {
		return openAlBufferId;
	}

	@Override
	public void destroy() {
		alDeleteBuffers(openAlBufferId);
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
