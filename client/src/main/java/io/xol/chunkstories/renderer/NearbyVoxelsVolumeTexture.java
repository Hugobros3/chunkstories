//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.Shader;
import io.xol.chunkstories.api.rendering.textures.Texture3D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;

public class NearbyVoxelsVolumeTexture {

	final WorldRenderer worldRenderer;
	final World world;
	
	private int size = 128;

	public NearbyVoxelsVolumeTexture(WorldRenderer worldRenderer) {
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
	}
	
	Texture3D test = null;
	
	int bx,by,bz;
	int offsetX, offsetY, offsetZ;
	
	public void update(RenderingInterface renderingContext) {
		if(test == null) {
			test = renderingContext.newTexture3D(TextureFormat.RGBA_8BPP, 32, 32, 32);
		}

		final int SIZE = size;
		final int mod = SIZE / 32;

		int offCenter = SIZE / 2;
		
		int chunkX = (int) ((renderingContext.getCamera().getCameraPosition().x() - offCenter) / 32);
		int chunkY = (int) ((renderingContext.getCamera().getCameraPosition().y() - offCenter) / 32);
		int chunkZ = (int) ((renderingContext.getCamera().getCameraPosition().z() - offCenter) / 32);
		
		offsetX = chunkX % mod;
		offsetY = chunkY % mod;
		offsetZ = chunkZ % mod;
		if(bx != chunkX || by != chunkY || bz != chunkZ) {
			
			bx = chunkX;
			by = chunkY;
			bz = chunkZ;
			ByteBuffer bb = MemoryUtil.memAlloc(4 * SIZE * SIZE * SIZE);
			
			byte[] empty = {0,0,0,0};
			
			Chunk zChunk = null;

			Vector4f col = new Vector4f();
			for(int a = 0; a*32 < SIZE; a++)
				for(int b = 0; b*32 < SIZE; b++)
					for(int c = 0; c*32 < SIZE; c++) {
						
						zChunk = worldRenderer.getWorld().getChunk(chunkX + a, chunkY + b, chunkZ + c);
						
						if(zChunk != null) {
							for(int z = 0; z < 32; z++)
								for(int y = 0; y < 32; y++) {
	
									int dx = (0 + a) % mod;
									int dy = (0 + b) % mod;
									int dz = (0 + c) % mod;
									
									bb.position(4 * ((dz*32 + z) * SIZE * SIZE + (dy*32 + y) * SIZE + 0 + dx*32));
									
									for(int x = 0; x < 32; x++) {
										CellData cell = zChunk.peek(x, y, z);
										Voxel voxel = cell.getVoxel();//zChunk.peekSimple(x, y, z);
										
										if(voxel.isAir() || voxel.getName().startsWith("glass") || !voxel.getDefinition().isSolid() && !voxel.getDefinition().isLiquid()) {
											bb.put(empty);
										} else {
											col.set(voxel.getVoxelTexture(VoxelSides.TOP, cell).getColor());
											if(col.w() < 1.0) {
												col.mul(new Vector4f(0.1f, 0.5f, 0.1f, 1.0f));
											}
											
											bb.put((byte)(int)(col.x() * 255));
											bb.put((byte)(int)(col.y() * 255));
											bb.put((byte)(int)(col.z() * 255));
											bb.put(voxel.getDefinition().getEmittedLightLevel() > 0 ? (byte) 20 : (byte) 1);
										} 
									}
								}
						}
					}
			
			bb.position(0);
			bb.limit(bb.capacity());
			test.uploadTextureData(SIZE, SIZE, SIZE, bb);
			test.setTextureWrapping(true);
			
			System.out.println("do an upload");
			MemoryUtil.memFree(bb);
		}
	}
	
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	public void setupForRendering(RenderingInterface renderingContext) {

		renderingContext.bindTexture3D("currentChunk", test);
		
		Shader shader = renderingContext.currentShader();
		shader.setUniform1i("voxel_size", size);
		shader.setUniform1f("voxel_sizef", 0.0f + size);
		shader.setUniform3f("voxelOffset", offsetX * 32, offsetY * 32, offsetZ * 32);
	}
}
