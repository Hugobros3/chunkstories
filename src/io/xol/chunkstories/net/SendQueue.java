package io.xol.chunkstories.net;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.net.packets.PacketDummy;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SendQueue extends Thread
{
	Queue<Packet> sendQueue = new ConcurrentLinkedQueue<Packet>();
	AtomicBoolean die = new AtomicBoolean(false);
	Semaphore workTodo = new Semaphore(0);

	PacketsProcessor processor;
	DataOutputStream out;

	//Reference to what we are sending stuff to, used in packet creation logic to look for implemented interfaces ( remote server, unlogged in client, logged in client etc )
	PacketDestinator destinator;

	//Used to buffer and compute the length synch packets to send

	//To avoid an HORRIBLE mess we have to have one temporary buffer per thread sending packets,
	//as we have no idea how many madmen will spawn zillions of concurrent threads to spam shit

	//NOTE: I just had my greatest breakthrought of the week with this, this field used to not be static and I had issues with world unloading on server
	// namely the ServerPlayer ( implementing WorldUser ) object would not get garbage collected and thus it's reference in the various world bits would hold
	// indefinitly. It appears that the cause was this ThreadLocal variable, because it is an inner class, it's always referencing it's parent/holder class
	// and thus the "destinator" field referencing the ServerPlayer ( once he authentificates himself ) would be accessible throught this object.
	// This becomes a problem with the ThreadLocal type : because ThreadLocal variables are local to threads they are stored in a Map inside the Thread's class, and
	// these are NOT gc'ed until the thread dies. Meaning that pushing a packet from the tick() loop caused this buffer to get intialized, making a permanent reference to the
	// player in the world ticking thread. The static qualifier prevents all this, I lost 5 hours to this ... 
	static ThreadLocal<SynchBuffer> synchBuffer = new ThreadLocal<SynchBuffer>()
	{
		@Override
		protected SynchBuffer initialValue()
		{
			return new SynchBuffer();
		}
	};

	//With a custom subclass because the two objects are needed and rely on each other
	static class SynchBuffer
	{

		SynchBuffer()
		{
			this.baos = new ByteArrayOutputStream(262144);
			this.outSynch = new DataOutputStream(baos);
		}

		public ByteArrayOutputStream baos;
		public DataOutputStream outSynch;
	}

	public SendQueue(PacketDestinator destinator, DataOutputStream out, PacketsProcessor processor)
	{
		this.destinator = destinator;
		this.out = out;
		this.processor = processor;

		this.setName("Send queue thread");
	}

	public void setDestinator(PacketDestinator destinator)
	{
		this.destinator = destinator;
	}

	/**
	 * Queue a packet for sending, no synchronisation needed
	 */
	public void queue(Packet packet)
	{
		if (die.get())
			return;

		if (packet instanceof PacketSynch)
		{
			//Get our thread's buffers
			SynchBuffer synchBuffer = SendQueue.synchBuffer.get();

			ByteArrayOutputStream baos = synchBuffer.baos;
			DataOutputStream outSynch = synchBuffer.outSynch;

			//Reset it
			baos.reset();
			try
			{
				//Send the packet in the buffer
				packet.send(destinator, outSynch);
				outSynch.flush();

				//How many bytes were written ?
				int packetSize = baos.size();

				//Make a dummy packet out of the stuff we got
				PacketSynchSendable sendablePacket = new PacketSynchSendable(packet.isSentFromClient());
				sendablePacket.data = baos.toByteArray();
				sendablePacket.packetLength = packetSize;
				sendablePacket.packetType = processor.getPacketId(packet);

				//Add that one instead of the real one
				sendQueue.add(sendablePacket);
			}
			catch (IOException e)
			{
				ChunkStoriesLogger.getInstance().error("Error : unable to buffer PacketSynch " + packet);
				e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
			}
			catch (UnknowPacketException e)
			{
				//Definitly not supposed to happen
				e.printStackTrace();
			}
		}
		else
			sendQueue.add(packet);

		workTodo.release();
		/*if(sleepy.get())
		{
			synchronized (this)
			{
				notifyAll();
			}
		}*/
	}

	@Override
	public void run()
	{
		while (!die.get())
		{
			workTodo.acquireUninterruptibly();

			Packet packet = sendQueue.poll();

			if (packet == null)
			{
				try
				{
					out.flush();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
				try
				{
					if (!(packet instanceof PacketDummy))
						processor.sendPacketHeader(out, packet);
					packet.send(destinator, out);
				}
				catch (IOException e)
				{
					//We don't care about that, it's the motd thing mostly
					//ChunkStoriesLogger.getInstance().error("Error : unable to send Packet");
				}
				catch (UnknowPacketException e)
				{
					//We care about that
					ChunkStoriesLogger.getInstance().error("Error : Unknown packet exception");
					e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				}
			//sleepy.set(false);
		}
	}

	public void kill()
	{
		try
		{
			out.close();
		}
		catch (IOException e)
		{
			//Really that's just disconnection
		}
		die.set(true);
		workTodo.release(10);
		synchronized (this)
		{
			notifyAll();
		}
	}
}
