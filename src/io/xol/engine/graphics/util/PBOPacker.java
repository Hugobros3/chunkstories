package io.xol.engine.graphics.util;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.ARBSync.*;
import org.lwjgl.opengl.GLSync;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.engine.concurrency.Fence;
import io.xol.engine.graphics.textures.Texture2D;

public class PBOPacker
{
	int bufferId;
	boolean alreadyReading = false;
	
	public PBOPacker()
	{
		bufferId = glGenBuffers();
	}
	
	public void copyTexure(Texture2D texture)
	{
		copyTexure(texture, 0);
	}
	
	public PBOPackerResult copyTexure(Texture2D texture, int level)
	{
		if(alreadyReading)
			throw new RuntimeException("You asked this PBO downloader to download a texture but you did not finish the last read.");
		
		alreadyReading = true;
		
		glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
		glBindTexture(GL_TEXTURE_2D, texture.getId());
		
		int width = texture.getWidth();
		int height = texture.getHeight();
		
		double pow = Math.pow(2, level);
		width =  (int)Math.ceil(width / pow);
		height = (int)Math.ceil(height / pow);
		glBufferData(GL_PIXEL_PACK_BUFFER, width * height * 4 * 3 , GL_STREAM_COPY);
		
		//glReadPixels(0,0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		
		//Reads the pixels of the texture to the fence.
		glGetTexImage(GL_TEXTURE_2D, level, GL_RGB, GL_FLOAT, 0);

		GLSync fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0x00);
		
		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
		
		return new PBOPackerResult(fence);
	}
	
	public class PBOPackerResult implements Fence {
		GLSync fence;
		boolean isTraversable = false;
		boolean readAlready = false;
		
		PBOPackerResult(GLSync fence)
		{
			this.fence = fence;
		}

		@Override
		public void traverse()
		{
			while(!isTraversable)
			{
				//Asks for wether the sync completed and timeouts in 1000ns or 1µs
				int waitReturnValue = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, 1000);
				
				//System.out.println("Waiting on GL fence");
				
				//Errors are considered ok
				if(waitReturnValue == GL_ALREADY_SIGNALED || waitReturnValue == GL_CONDITION_SATISFIED || waitReturnValue == GL_WAIT_FAILED)
					break;
			}
		}
		
		public boolean isTraversable()
		{
			//Don't do these calls for nothing
			if(isTraversable)
				return true;
			
			int syncStatus = glGetSynci(fence, GL_SYNC_STATUS);
			isTraversable = syncStatus == GL_SIGNALED;
			
			return isTraversable;
		}
		
		public ByteBuffer readPBO()
		{
			if(readAlready)
				throw new RuntimeException("Tried to read a PBOPackerResult twice !");
			
			//Traverses the sync object first
			traverse();
			
			glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
			
			//Map the buffer and read it
			ByteBuffer gpuBuffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, null);
			
			ByteBuffer freeBuffer = BufferUtils.createByteBuffer(gpuBuffer.capacity());
			int free = freeBuffer.remaining();
			freeBuffer.put(gpuBuffer);
			int freeNow = freeBuffer.remaining();
			//System.out.println("Read "+(free - freeNow)+" bytes from the PBO");
			
			//Unmpapps the buffer 
		    glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
			glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
			
			//Destroys the useless fence
			glDeleteSync(fence);
			
			PBOPacker.this.alreadyReading = false;
			this.readAlready = true;
			
			return freeBuffer;
		}
	}
	
	public void destroy()
	{
		glDeleteBuffers(bufferId);
	}
}
