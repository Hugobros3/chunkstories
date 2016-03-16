package io.xol.chunkstories.renderer.chunks;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL15.*;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Responsible of holding all rendering information about one chunk
 * ie : VBO creation, uploading and deletion, as well as decals
 * @author Hugo
 *
 */
public class ChunkRenderData
{
	public CubicChunk chunk;
	public int vboId = -1;
	
	public int vboSizeFullBlocks;
	public int vboSizeWaterBlocks;
	public int vboSizeCustomBlocks;
	
	public ByteBufferPool pool;
	public int byteBufferPoolId = -1;
	
	public boolean isUploaded = false;
	
	public ChunkRenderData(ByteBufferPool pool, CubicChunk chunk)
	{
		this.pool = pool;
		this.chunk = chunk;
	}
	
	public boolean isUploaded()
	{
		return isUploaded;
	}
	
	/**
	 * Uploads the ByteBuffer contents and frees it
	 */
	public void upload()
	{
		//Check VBO exists
		if (vboId == -1)
			vboId = glGenBuffers();
		
		//Upload data
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, pool.accessByteBuffer(byteBufferPoolId), GL_STATIC_DRAW);

		//Release BB
		pool.releaseByteBuffer(byteBufferPoolId);
		byteBufferPoolId = -1;
		
		isUploaded = true;
	}
	
	/**
	 * Frees the ressources allocated to this ChunkRenderData
	 */
	public void free()
	{
		//Make sure we freed the byteBuffer
		if(byteBufferPoolId != -1)
			pool.releaseByteBuffer(byteBufferPoolId);
		byteBufferPoolId = -1;
		//Deallocate the VBO
		if(vboId != -1)
			glDeleteBuffers(vboId);
	}
	
	/**
	 * Thread-safe way to free the ressources
	 */
	public void markForDeletion()
	{
		addToDeletionQueue(this);
	}
	
	public static Set<ChunkRenderData> uselessChunkRenderDatas = ConcurrentHashMap.newKeySet();
	
	public static void deleteUselessVBOs()
	{
		Iterator<ChunkRenderData> i = uselessChunkRenderDatas.iterator();
		while(i.hasNext())
		{
			ChunkRenderData crd = i.next();
			crd.free();
			i.remove();
		}
	}
	
	public static void addToDeletionQueue(ChunkRenderData crd)
	{
		uselessChunkRenderDatas.add(crd);
	}

	public long getVramSize()
	{
		return vboSizeFullBlocks * 16 + vboSizeWaterBlocks * 24 + vboSizeCustomBlocks * 24;
	}
}
