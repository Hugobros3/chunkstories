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
	//AtomicBoolean sleepy = new AtomicBoolean(false);
	
	PacketsProcessor processor;
	DataOutputStream out;
	
	//Reference to what we are sending stuff to, used in packet creation logic to look for implemented interfaces ( remote server, unlogged in client, logged in client etc )
	PacketDestinator destinator;
	
	//Used to buffer and compute the length synch packets to send
	
	//To avoid an HORRIBLE mess we have to have one temporary buffer per thread sending packets,
	//as we have no idea how many madmen will spawn zillions of concurrent threads to spam lolz
	//we do the classic thread local approach
	ThreadLocal<SynchBuffer> synchBuffer = new ThreadLocal<SynchBuffer>()
	{
		@Override
		protected SynchBuffer initialValue()
		{
			return new SynchBuffer();
		}
	};
	//With a custom subclass because the two objects are needed and rely on each other
	class SynchBuffer {

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
		if(die.get())
			return;
		
		if(packet instanceof PacketSynch)
		{
			//Get our thread's buffers
			SynchBuffer synchBuffer = this.synchBuffer.get();
			
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
				ChunkStoriesLogger.getInstance().error("Error : unable to buffer PacketSynch "+packet);
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
		while(!die.get())
		{
			workTodo.acquireUninterruptibly();
			
			Packet packet = null;
			
			if(sendQueue.size() > 0)
				packet = sendQueue.poll();
			
			if(packet == null)
			{
				/*try
				{
					synchronized (this)
					{
						//Wait if no more job to do.
						sleepy.set(true);
						wait();
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}*/
			}
			else
				try
				{
					if(!(packet instanceof PacketDummy))
						processor.sendPacketHeader(out, packet);
					packet.send(destinator, out);
					out.flush();
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
		synchronized (this)
		{
			notifyAll();
		}
	}
}
