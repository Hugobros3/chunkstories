package io.xol.engine.graphics.util;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.ARBSync.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.target.RenderTargetManager;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.fbo.FrameBufferObjectGL;
import io.xol.engine.graphics.textures.Texture2DGL;

public class PBOPacker
{
	RenderTargetAttachementsConfiguration fbo = new FrameBufferObjectGL(null);
	
	int bufferId;
	boolean alreadyReading = false;
	
	public PBOPacker()
	{
		bufferId = glGenBuffers();
	}
	
	public void copyTexure(Texture2DGL texture)
	{
		copyTexure(texture, 0);
	}
	
	public PBOPackerResult copyTexure(Texture2DGL texture, int level)
	{
		if(alreadyReading)
			throw new RuntimeException("You asked this PBO downloader to download a texture but you did not finish the last read.");
		
		alreadyReading = true;
	
		glBindBuffer(GL_PIXEL_PACK_BUFFER, bufferId);
		//glBindTexture(GL_TEXTURE_2D, texture.getId());
		
		int width = texture.getWidth();
		int height = texture.getHeight();
		
		double pow = Math.pow(2, level);
		width =  (int)Math.ceil(width / pow);
		height = (int)Math.ceil(height / pow);
		
		//Allocates space for the read
		glBufferData(GL_PIXEL_PACK_BUFFER, width * height * 4 * 3 , GL_STREAM_COPY);

		//Obtains ref to RTM
		RenderTargetManager rtm = Client.getInstance().getGameWindow().getRenderingContext().getRenderTargetManager();
		
		RenderTargetAttachementsConfiguration previousFB = rtm.getCurrentConfiguration();
		rtm.setConfiguration(fbo);
		fbo.setColorAttachements(texture.getMipLevelAsRenderTarget(level));
		fbo.setEnabledRenderTargets(true);
		
		//rtm.clearBoundRenderTargetAll();
		glReadBuffer(GL_COLOR_ATTACHMENT0);
		
		//Reads the pixels of the texture to the PBO.
		glReadPixels(0, 0, width, height, GL_RGB, GL_FLOAT, 0);
		
		//slower method: 
		/*TextureFormat format = texture.getType();
		System.out.println(format.getBytesPerTexel());
		glGetTexImage(GL_TEXTURE_2D, level, format.getFormat(), format.getType(), 0);*/

		final long fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0x00);
		
		//Puts everything back into place
		glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
		rtm.setConfiguration(previousFB);
		
		
		//System.out.println((endT-startT)/1000+"µs");
		
		return new PBOPackerResult(fence);
	}
	
	public class PBOPackerResult implements Fence {
		final long fence;
		boolean isTraversable = false;
		boolean readAlready = false;
		
		PBOPackerResult(long fence)
		{
			this.fence = fence;
		}

		@Override
		public void traverse()
		{
			while(!isTraversable)
			{
				//Asks for wether the sync completed and timeouts in 1000ns or 1µs
				int waitReturnValue = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, 1);
				
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
			
			//int syncStatus = glGetSynci(fence, GL_SYNC_STATUS);
			try(MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer ib = stack.mallocInt(1);
				glGetSynci(fence, GL_SYNC_STATUS, ib);
				isTraversable = ib.get(0) == GL_SIGNALED;
			}
			
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
			freeBuffer.put(gpuBuffer);
			//System.out.println("Read "+(free - freeNow)+" bytes from the PBO in "+(endT-startT)/1000+" µs");
			
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
