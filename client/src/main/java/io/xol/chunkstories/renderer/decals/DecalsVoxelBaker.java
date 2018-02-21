package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerCubic;
import io.xol.chunkstories.api.rendering.world.chunk.vertexlayout.IntricateLayoutBaker;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsVoxelBaker extends IntricateLayoutBaker implements VoxelBakerCubic
{
	protected DecalsVoxelBaker(ClientContent content, ByteBuffer output) {
		super(content, output);
	}

	ByteBuffer byteBuffer;
	int cx, cy, cz;
	
	public void setChunk(Chunk c) {
		cx = c.getChunkX();
		cy = c.getChunkY();
		cz = c.getChunkZ();
	}

	@Override
	public void beginVertex(int i0, int i1, int i2) {
		this.beginVertex((float)i0, (float)i1, (float)i2);
	}

	@Override
	//Offset each vertex with the chunk location (since we draw all decals in a single call and we don't keep that offset anywhere else )
	public void beginVertex(float f0, float f1, float f2)
	{
		super.beginVertex(cx * 32 + f0, cy * 32 + f1, cz * 32 + f2);
		/*byteBuffer.putFloat(f0 + cx * 32);
		byteBuffer.putFloat(f1 + cy * 32);
		byteBuffer.putFloat(f2 + cz * 32);*/
	}

	@Override
	public void endVertex() {
		//We spit out this layout to feed the TrianglesClipper class
		output.putFloat(currentVertex.x);
		output.putFloat(currentVertex.y);
		output.putFloat(currentVertex.z);
		
		output.putFloat(normal.x);
		output.putFloat(normal.y);
		output.putFloat(normal.z);
	}

	/*@Override
	public void addNormalsInt(int i0, int i1, int i2, byte extra)
	{
		if(byteBuffer.position() == byteBuffer.capacity())
			return;
		byteBuffer.putFloat((float)(i0 + 1) / 512 - 1);
		byteBuffer.putFloat((float)(i1 + 1) / 512 - 1);
		byteBuffer.putFloat((float)(i2 + 1) / 512 - 1);
		//this.addVerticeFloat((i0 + 1) / 512 - 1, (i1 + 1) / 512 - 1, (i2 + 1) / 512 - 1);
	}*/
}
