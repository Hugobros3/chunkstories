package io.xol.chunkstories.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LengthAwareBufferedIOHelper
{
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
		}

		public ByteArrayOutputStream baos;
	}
	
	public static class LengthAwareOutputStream extends DataOutputStream {

		ByteArrayOutputStream baos;
		LengthAwareOutputStream(ByteArrayOutputStream baos)
		{
			super(baos);
			this.baos = baos;
		}
		
		public void writeTheStuff(DataOutputStream actualOutput) throws IOException {
			
			byte[] buffer = baos.toByteArray();
			actualOutput.writeInt(buffer.length);
			actualOutput.write(buffer);
			baos.reset();
		}
	}
	
	public static LengthAwareOutputStream getLengthAwareOutput() {
		
		ByteArrayOutputStream baos = synchBuffer.get().baos;
		baos.reset();
		
		return new LengthAwareOutputStream(baos);
	}
	
	public static class LengthAwareInputStream extends DataInputStream {

		public LengthAwareInputStream(InputStream arg0)
		{
			super(arg0);
		}
		
	}
	
	public static LengthAwareInputStream getLengthAwareInput(int bufferLength, DataInputStream dis) throws IOException {
		//int bufferLength = dis.readInt();
		
		byte[] buffer = new byte[bufferLength];
		dis.readFully(buffer);
		
		return new LengthAwareInputStream(new ByteArrayInputStream(buffer));
	}
}
