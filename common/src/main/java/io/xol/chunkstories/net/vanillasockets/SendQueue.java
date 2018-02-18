package io.xol.chunkstories.net.vanillasockets;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.net.PacketOutgoing;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.concurrency.TrivialFence;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/** The job of this thread is to write datagrams to an output stream. Not much in the way of complexity there */
public class SendQueue extends Thread {
	private final BlockingQueue<PacketOutgoing> sendQueue = new LinkedBlockingQueue<PacketOutgoing>();
	private final DataOutputStream outputStream;

	private static final Logger logger = LoggerFactory.getLogger("net");

	public Logger logger() {
		return logger;
	}

	public SendQueue(DataOutputStream out) {
		this.outputStream = out;
		this.setName("Send queue thread");
	}

	//Those two aren't going anywhere
	PacketOutgoing DIE = new PacketOutgoing() {
		@Override
		public void write(DataOutputStream out) throws IOException {
			throw new UnsupportedOperationException();
		}
	};
	class Flush implements PacketOutgoing {
		
		@Override
		public void write(DataOutputStream out) throws IOException {
			throw new UnsupportedOperationException();
		}

		SimpleFence fence = new SimpleFence();
	}

	public void queue(PacketOutgoing packet) {
		sendQueue.add(packet);
	}

	Lock deathLock = new ReentrantLock();
	boolean dead = false;

	public Fence flush() {
		deathLock.lock();
		if (dead) {
			deathLock.unlock();
			return new TrivialFence();
		}

		Flush flush = new Flush();
		sendQueue.add(flush);

		deathLock.unlock();
		return flush.fence;
	}

	@Override
	public void run() {
		while (true) {
			PacketOutgoing packet = null;

			try {
				packet = sendQueue.take();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			if (packet == DIE) {
				// Kill request ? accept gracefully our fate
				break;
			}

			if (packet == null) {
				System.out.println("ASSERTION FAILED : THE SEND QUEUE CAN'T CONTAIN NULL PACKETS.");
				System.exit(-1);
			} else if (packet instanceof Flush) {
				try {
					outputStream.flush();
					((Flush) packet).fence.signal();
				} catch (IOException e) {
					// That's basically terminated connection exceptions
					((Flush) packet).fence.signal();
					disconnect("Broken pipe: Unable to flush: " + e.getMessage());
					break;
				}
			} else
				try {
					packet.write(outputStream);
				} catch (IOException e) {
					// We don't care about that, it's the motd thing mostly
					disconnect("Broken pipe: Unable to send packet: " + e.getMessage());
					break;
				}
		}

		deathLock.lock();
		dead = true;
		deathLock.unlock();

		// We do a final round of discarding flush requests, to avoid deadlocking
		PacketOutgoing packet = null;
		while (true) {
			packet = sendQueue.poll();
			if (packet == null)
				break;

			// We signal all the remaining flush fluff
			if (packet instanceof Flush)
				((Flush) packet).fence.signal();

			packet = null;
		}

		try {
			outputStream.close();
		} catch (IOException e) {
			// Really that's just disconnection
		}
	}

	private void disconnect(String string) {
		deathLock.lock();
		dead = true;
		deathLock.unlock();
		//connection.disconnect(string);
	}

	public void kill() {
		sendQueue.add(DIE);

		synchronized (this) {
			notifyAll();
		}
	}
}
