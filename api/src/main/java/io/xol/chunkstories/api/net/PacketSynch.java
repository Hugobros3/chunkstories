package io.xol.chunkstories.api.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Synch packets send prepared packets instead of building it at sendtime
 * Synch packets are read at the beginning of a tick and sent at the end
 * Synch packets are meant to be short ; their length is computed when being sent and can be variable
 * Synch packets should never be longer than 256KiB !
 */
public abstract class PacketSynch extends Packet
{
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
	
	public DataOutputStream getSynchPacketOutputStream()
	{
		return synchBuffer.get().outSynch;
	}
	
	public void finalizeSynchPacket() throws IOException
	{
		synchBuffer.get().baos.flush();
		preparedMessage = synchBuffer.get().baos.toByteArray();
		synchBuffer.get().baos.reset();
	}
	
	byte[] preparedMessage;
	
	@Override
	public final void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeInt(preparedMessage.length);
		out.write(preparedMessage);
	}

}
