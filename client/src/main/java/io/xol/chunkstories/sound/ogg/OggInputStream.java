//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.ogg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class OggInputStream extends InputStream {
	InputStream inputStream;

	boolean reachedEnd = true;

	SyncState syncState = new SyncState();
	StreamState streamState = new StreamState();
	Page page = new Page();
	Packet packet = new Packet();
	public Info info = new Info();
	Comment comment = new Comment();
	DspState dspState = new DspState();
	Block currentBlock = new Block(dspState);

	public OggInputStream(InputStream inputStream) throws IOException {
		this.inputStream = inputStream;
		if (inputStream == null)
			throw new IOException("Null input stream !");
		syncState.init();
		readPCM();
	}

	byte[] buffer;
	int bytes;

	int convsize = 8192;
	private byte[] convbuffer = new byte[convsize];

	private boolean getPageAndPacket() {
		// grab some data at the head of the stream. We want the first page
		// (which is guaranteed to be small and only contain the Vorbis
		// stream initial header) We need the first page to getVoxelComponent the stream
		// serialno.

		// submit a 4k block to libvorbis' Ogg layer
		int index = syncState.buffer(4096);

		buffer = syncState.data;
		if (buffer == null) {
			reachedEnd = true;
			return false;
		}

		try {
			bytes = inputStream.read(buffer, index, 4096);
		} catch (Exception e) {
			System.out.println("Failure reading in vorbis");
			System.out.println(e);
			reachedEnd = true;
			return false;
		}
		syncState.wrote(bytes);

		// Get the first page.
		if (syncState.pageout(page) != 1) {
			// have we simply run out of data? If so, we're done.
			if (bytes < 4096)
				return false;

			// error case. Must not be Vorbis data
			System.out.println("Input does not appear to be an Ogg bitstream.");
			reachedEnd = true;
			return false;
		}

		// Get the serial number and set up the rest of decode.
		// serialno first; use it to set up a logical stream
		streamState.init(page.serialno());

		// extract the initial header from the first page and verify that the
		// Ogg bitstream is in fact Vorbis data

		// I handle the initial header first instead of just having the code
		// read all three Vorbis headers at once because reading the initial
		// header is an easy way to identify a Vorbis bitstream and it's
		// useful to see that functionality seperated out.

		info.init();
		comment.init();
		if (streamState.pagein(page) < 0) {
			// error; stream version mismatch perhaps
			System.out.println("Error reading first page of Ogg bitstream data.");
			reachedEnd = true;
			return false;
		}

		if (streamState.packetout(packet) != 1) {
			// no page? must not be vorbis
			System.out.println("Error reading initial header packet.");
			reachedEnd = true;
			return false;
		}

		if (info.synthesis_headerin(comment, packet) < 0) {
			// error case; not a vorbis header
			System.out.println("This Ogg bitstream does not contain Vorbis audio data.");
			reachedEnd = true;
			return false;
		}

		// At this point, we're sure we're Vorbis. We've set up the logical
		// (Ogg) bitstream decoder. Get the comment and codebook headers and
		// set up the Vorbis decoder

		// The next two packets in order are the comment and codebook headers.
		// They're likely large and may span multiple pages. Thus we reead
		// and submit data until we getVoxelComponent our two pacakets, watching that no
		// pages are missing. If a page is missing, error out; losing a
		// header page is the only place where missing data is fatal. */

		int i = 0;
		while (i < 2) {
			while (i < 2) {

				int result = syncState.pageout(page);
				if (result == 0)
					break; // Need more data
				// Don't complain about missing or corrupt data yet. We'll
				// catch it at the packet output phase

				if (result == 1) {
					streamState.pagein(page); // we can ignore any errors here
					// as they'll also become apparent
					// at packetout
					while (i < 2) {
						result = streamState.packetout(packet);
						if (result == 0)
							break;
						if (result == -1) {
							// Uh oh; data at some point was corrupted or missing!
							// We can't tolerate that in a header. Die.
							System.out.println("Corrupt secondary header.  Exiting.");
							reachedEnd = true;
							return false;
						}

						info.synthesis_headerin(comment, packet);
						i++;
					}
				}
			}
			// no harm in not checking before adding more
			index = syncState.buffer(4096);
			buffer = syncState.data;
			try {
				bytes = inputStream.read(buffer, index, 4096);
			} catch (Exception e) {
				System.out.println("Failed to read Vorbis: ");
				System.out.println(e);
				reachedEnd = true;
				return false;
			}
			if (bytes == 0 && i < 2) {
				System.out.println("End of file before finding all Vorbis headers!");
				reachedEnd = true;
				return false;
			}
			syncState.wrote(bytes);
		}

		convsize = 4096 / info.channels;

		// OK, got and parsed all three headers. Initialize the Vorbis
		// packet->PCM decoder.
		dspState.synthesis_init(info); // central decode state
		currentBlock.init(dspState); // local state for most of the decode
		// so multiple block decodes can
		// proceed in parallel. We could init
		// multiple vorbis_block structures
		// for vd here

		return true;
	}

	boolean inited = false;
	boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

	private void readPCM() throws IOException {
		boolean wrote = false;

		while (true) { // we repeat if the bitstream is chained
			if (reachedEnd) {
				if (!getPageAndPacket()) {
					break;
				}
				reachedEnd = false;
			}

			if (!inited) {
				inited = true;
				return;
			}

			float[][][] _pcm = new float[1][][];
			int[] _index = new int[info.channels];
			// The rest is just a straight decode loop until end of stream
			while (!reachedEnd) {
				while (!reachedEnd) {
					int result = syncState.pageout(page);

					if (result == 0) {
						break; // need more data
					}

					if (result == -1) { // missing or corrupt data at this page position
						System.out.println("Corrupt or missing data in bitstream; continuing...");
					} else {
						streamState.pagein(page); // can safely ignore errors at
						// this point
						while (true) {
							result = streamState.packetout(packet);

							if (result == 0)
								break; // need more data
							if (result == -1) { // missing or corrupt data at this page position
								// no reason to complain; already complained above
							} else {
								// we have a packet. Decode it
								int samples;
								if (currentBlock.synthesis(packet) == 0) { // test for success!
									dspState.synthesis_blockin(currentBlock);
								}

								// **pcm is a multichannel float vector. In stereo, for
								// example, pcm[0] is left, and pcm[1] is right. samples is
								// the size of each channel. Convert the float values
								// (-1.<=range<=1.) to whatever PCM format and write it out

								while ((samples = dspState.synthesis_pcmout(_pcm, _index)) > 0) {
									float[][] pcm = _pcm[0];
									// boolean clipflag = false;
									int bout = (samples < convsize ? samples : convsize);

									// convert floats to 16 bit signed ints (host order) and
									// interleave
									for (int i = 0; i < info.channels; i++) {
										int ptr = i * 2;
										// int ptr=i;
										int mono = _index[i];
										for (int j = 0; j < bout; j++) {
											int val = (int) (pcm[i][mono + j] * 32767.);
											// might as well guard against clipping
											if (val > 32767) {
												val = 32767;
											}
											if (val < -32768) {
												val = -32768;
											}
											if (val < 0)
												val = val | 0x8000;

											if (bigEndian) {
												convbuffer[ptr] = (byte) (val >>> 8);
												convbuffer[ptr + 1] = (byte) (val);
											} else {
												convbuffer[ptr] = (byte) (val);
												convbuffer[ptr + 1] = (byte) (val >>> 8);
											}
											ptr += 2 * (info.channels);
										}
									}

									int bytesToWrite = 2 * info.channels * bout;
									if (bytesToWrite >= pcmBuffer.remaining()) {
										System.out.println(
												"Read block from OGG that was too big to be buffered: " + bytesToWrite);
									} else {
										pcmBuffer.put(convbuffer, 0, bytesToWrite);
									}

									wrote = true;
									dspState.synthesis_read(bout); // tell libvorbis how
									// many samples we
									// actually consumed
								}
							}
						}
						if (page.eos() != 0) {
							reachedEnd = true;
						}

						if ((!reachedEnd) && (wrote)) {
							return;
						}
					}
				}

				if (!reachedEnd) {
					bytes = 0;
					int index = syncState.buffer(4096);
					if (index >= 0) {
						buffer = syncState.data;
						try {
							bytes = inputStream.read(buffer, index, 4096);
						} catch (Exception e) {
							System.out.println("Failure during vorbis decoding");
							System.out.println(e);
							reachedEnd = true;
							return;
						}
					} else {
						bytes = 0;
					}
					syncState.wrote(bytes);
					if (bytes == 0) {
						reachedEnd = true;
					}
				}
			}

			// clean up this logical bitstream; before exit we see if we're
			// followed by another [chained]
			streamState.clear();

			// ogg_page and ogg_packet structs always point to storage in
			// libvorbis. They're never freed or manipulated directly

			currentBlock.clear();
			dspState.clear();
			info.clear(); // must be called last
		}

		// OK, clean up the framer
		syncState.clear();
		reachedEnd = true;
	}

	private int readIndex;
	private ByteBuffer pcmBuffer = BufferUtils.createByteBuffer(4096 * 500);

	@Override
	public int read() throws IOException {
		if (readIndex >= pcmBuffer.position()) {
			pcmBuffer.clear();
			readPCM();
			readIndex = 0;
		}
		if (readIndex >= pcmBuffer.position()) {
			return -1;
		}

		int value = pcmBuffer.get(readIndex);
		if (value < 0) {
			value = 256 + value;
		}
		readIndex++;

		return value;
	}

	public boolean atEnd() {
		return reachedEnd && (readIndex >= pcmBuffer.position());
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			try {
				int value = read();
				if (value >= 0) {
					b[i] = (byte) value;
				} else {
					if (i == 0) {
						return -1;
					} else {
						return i;
					}
				}
			} catch (IOException e) {
				System.out.println(e);
				return i;
			}
		}

		return len;
	}

	public int getChannel() {
		return info.channels;
	}

	public int getRate() {
		return info.rate;
	}
}
