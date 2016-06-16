package io.xol.chunkstories.core.generator;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.biomes.Biome;
import io.xol.chunkstories.world.biomes.BiomeIndex;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.generator.util.SeededRandomNumberTranslator;
import io.xol.chunkstories.world.generator.util.SeededSimplexNoiseGenerator;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PerlinWorldGenerator extends WorldGenerator
{
	// This is the first ever voxel World Generator by XolioWare Interactive
	SeededSimplexNoiseGenerator ssng;
	SeededRandomNumberTranslator srnt;

	int sic = 2;

	@Override
	public void initialize(World w)
	{
		super.initialize(w);
		ssng = new SeededSimplexNoiseGenerator(w.getWorldInfo().getSeed());
		srnt = new SeededRandomNumberTranslator(w.getWorldInfo().getSeed());
		sic = world.getSizeInChunks();
	}

	public int sanitizeChunkCoord(int chunkX)
	{
		chunkX = chunkX % sic;
		if (chunkX < 0)
			chunkX += sic;
		return chunkX;
	}

	class Cave
	{
		int[][] coords;
		boolean[] care;
	}

	@Override
	public int getDataAt(int a, int b)
	{
		int cx = a / 32;
		int cz = b / 32;
		a %= 32;
		b %= 32;

		float humidity = (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 987, 148, sic / 64f, sic / 64f) + 1) / 2f;
		float temperature = (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 32649, -877, sic / 128f, sic / 128f) + 1) / 2f;

		Biome biome = BiomeIndex.getBiomeFor(humidity, temperature, 15);
		if (getHeightAt(a, b) <= 128)
			return biome.getFluidTile();
		return biome.getGroundTile(1, 1);
	}
	
	@Override
	public int getHeightAt(int a, int b)
	{
		int cx = a / 32;
		int cz = b / 32;
		a %= 32;
		b %= 32;

		float height = 50;
		//double originalRoughtness = (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 10154, 181, sic / 128f, sic / 128f));
		//originalRoughtness += 0.8;
		//float cliffness = (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 6679, -784, sic / 32f, sic / 32f) + 1) + 1;
		//double roughtness = Math.pow(originalRoughtness, cliffness);// roughtness*roughtness;

		//roughtness = Math.min(roughtness, 1.1);
		//roughtness = Math.max(roughtness, 0);
		float roughtness = 1;
		height += (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 4751, 12111, sic / 128f, sic / 128f) + 0.8) * 40;
		height += (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 5258, -56, sic / 128f, sic / 128f) + 0.8) * 80 * roughtness;
		//height += (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 5258, -56, sic / 512f, sic / 512f) + 0.8) * 40 * Math.max(0, 1 - roughtness);
		height -= Math.abs(ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 5258, -56, sic / 64f, sic / 64f) + 0.8) * 40 * Math.max(0, 1 - roughtness);
		height += (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 2814, -158, sic / 64f, sic / 64f) + 0.8) * 40 * Math.max(roughtness - 0.80, 0) * 1.5;
		//double cliffness2 = Math.max(roughtness - 0.95, 0);
		height += Math.abs(ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 153, 4874, sic / 64f, sic / 64f) + 0.8) * 20 * 0.5f * 2.5;
		// height+=(ssng.looped_noise((cx*32+a),(cz*32+b),sic*32,-154,0,sic/32f,sic/32f)+1)*15*(roughtness);
		//height += (ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 6997, 0, sic / 4f, sic / 4f) + 1) * roughtness;
		// height = ocean;
		int to = (int) Math.min(1024, height);
		to = (int) Math.max(5, height);

		if(to < 127)
			to = 127;
		
		return to;
	}

	@Override
	public CubicChunk generateChunk(int cx, int cy, int cz)
	{
		// System.out.println("World init to :"+world);
		CubicChunk c = new CubicChunk(world, cx, cy, cz);
//		// c.dataPointer = world.chunksData.malloc();
//		// Check the fucking nearby chunks
//
//		/*
//		 * List<Cave> caves = new ArrayList<Cave>(); //Cave generation for(int a
//		 * = cx-1; a < cx+1; a++) for(int b = cz-1; b < cz+1; b++) { int ccx =
//		 * sanitizeChunkCoord(a); int ccz = sanitizeChunkCoord(b);
//		 * if(srnt.getRandomChanceForChunk(ccx, ccz) > 850) { Cave cave = new
//		 * Cave(); int length =
//		 * 4+(int)(srnt.getRandomChanceForChunkPlusModifier(
//		 * ccx,ccz,3674)/1000f*16)*4; cave.coords = new int[length/4][3];
//		 * cave.care = new boolean[length/4]; float xcoord =
//		 * ccx*32+(int)(srnt.getRandomChanceForChunkPlusModifier
//		 * (ccx,ccz,2785)/1000f*32)%32; float ycoord =
//		 * (int)(srnt.getRandomChanceForChunkPlusModifier
//		 * (ccx,ccz,871)/1000f*74+4)%127; float zcoord =
//		 * ccz*32+(int)(srnt.getRandomChanceForChunkPlusModifier
//		 * (ccx,ccz,1587)/1000f*32)%32;
//		 * 
//		 * double directionH =
//		 * srnt.getRandomChanceForChunkPlusModifier(ccx,ccz,237
//		 * )/1000f*2*Math.PI; double directionV =
//		 * srnt.getRandomChanceForChunkPlusModifier
//		 * (ccx,ccz,974)/1000f*2*Math.PI/5; cave.coords[0] = new
//		 * int[]{(int)xcoord,(int)ycoord,(int)zcoord}; boolean concerned =
//		 * false; for(int i = 4; i < length; i+=4) {
//		 * 
//		 * directionH+=(srnt.getRandomChanceForChunkPlusModifier(ccx,ccz,237)-
//		 * 500 )/1000f/2f;
//		 * directionV+=(srnt.getRandomChanceForChunkPlusModifier(ccx,
//		 * ccz,981)-500)/1000f;
//		 * 
//		 * xcoord+=Math.sin(directionH)*4; ycoord+=Math.sin(directionV)*4;
//		 * zcoord+=Math.cos(directionH)*4; boolean care =
//		 * (LoopingMathHelper.moduloDistance((int)xcoord, cx*32+16, sic) < 32 &&
//		 * LoopingMathHelper.moduloDistance((int)zcoord, cz*32+16, sic) < 32);
//		 * if(care) concerned = true; cave.care[i/4] = care; cave.coords[i/4] =
//		 * new int[]{(int)xcoord,(int)ycoord,(int)zcoord};
//		 * //System.out.println(xcoord+":"+ycoord+":"+zcoord); }
//		 * 
//		 * if(concerned) caves.add(cave); } }
//		 */
//		for (int a = 0; a < 32; a++)
//			for (int b = 0; b < 32; b++)
//			{
//				// Terrain generation
//				int height = getHeightAt(cx * 32 + a, cz * 32 + b);
//
//				float humidity = (float) ((ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 987, 148, sic / 64f, sic / 64f) + 1) / 2d);
//				float temperature = (float) ((ssng.looped_noise((cx * 32 + a), (cz * 32 + b), sic * 32, 32649, -877, sic / 128f, sic / 128f) + 1) / 2d);
//
//				int to = Math.max(5, height);
//				int i;
//				int water_level = 128;
//				Biome biome = BiomeIndex.getBiomeFor(humidity, temperature, cy * 32);
//				for (i = cy * 32; i < cy * 32 + 32; i++)
//				{
//					if (i <= to)
//					{
//						/*
//						 * if(i > to-2 && cliffness3 > 0.05 && cliffness3 <
//						 * 0.20) c.setDataAt(a, i-cy*32, b,
//						 * VoxelFormat.format(1, 0, 0, 0)); else
//						 */if (i > to - 4)
//							c.setDataAt(a, i - cy * 32, b, VoxelFormat.format(biome.getGroundTile(to, i), 0, 0, 0));
//						else
//							c.setDataAt(a, i - cy * 32, b, VoxelFormat.format(1, 0, 0, 0));
//					}
//					else if (i < water_level)
//						c.setDataAt(a, i - cy * 32, b, VoxelFormat.format(biome.getFluidTile(), 0, 0, 0));
//					else if (i == to + 1)
//					{
//						c.setDataAt(a, i - cy * 32, b, VoxelFormat.format(biome.getTopTile(i), 0, 0, 0));
//					}
//
//				}
//				if (to >= cy * 32 && to < cy * 32 + 32)
//				{
//					GenerableStructure treeType = biome.getTreeType(srnt, a + cx * 32, to, b + cz * 32);
//					if (treeType != null)
//					{
//						c.structures.add(treeType);
//					}
//				}
//
//				// Structures evaluation
//				for (GenerableStructure str : c.structures)
//				{
//					str.draw(c);
//				}
//				CubicChunk side;
//				side = world.getChunk(c.chunkX, c.chunkY + 1, c.chunkZ, false);
//				if (side != null)
//				{
//					boolean changed = false;
//					for (GenerableStructure str : c.structures)
//					{
//						str.draw(side);
//						changed = true;
//					}
//					if (changed)
//						side.need_render.set(true);
//					for (GenerableStructure str : side.structures)
//					{
//						str.draw(c);
//					}
//				}
//
//				side = world.getChunk(c.chunkX, c.chunkY - 1, c.chunkZ, false);
//				if (side != null)
//				{
//					boolean changed = false;
//					for (GenerableStructure str : c.structures)
//					{
//						str.draw(side);
//						changed = true;
//					}
//					if (changed)
//						side.need_render.set(true);
//					for (GenerableStructure str : side.structures)
//					{
//						str.draw(c);
//					}
//				}
//				//
//				for (int t = -1; t < 2; t++)
//				{
//					for (int u = -1; u < 2; u++)
//					{
//						side = world.getChunk(c.chunkX + t, c.chunkY + 1, c.chunkZ + u, false);
//						if (side != null)
//						{
//							boolean changed = false;
//							for (GenerableStructure str : c.structures)
//							{
//								str.draw(side);
//								changed = true;
//							}
//							if (changed)
//								side.need_render.set(true);
//							for (GenerableStructure str : side.structures)
//							{
//								str.draw(c);
//							}
//						}
//						side = world.getChunk(c.chunkX + t, c.chunkY - 1, c.chunkZ + u, false);
//						if (side != null)
//						{
//							boolean changed = false;
//							for (GenerableStructure str : c.structures)
//							{
//								str.draw(side);
//								changed = true;
//							}
//							if (changed)
//								side.need_render.set(true);
//							for (GenerableStructure str : side.structures)
//							{
//								str.draw(c);
//							}
//						}
//						if (u != 0 || t != 0)
//						{
//							side = world.getChunk(c.chunkX + t, c.chunkY, c.chunkZ + u, false);
//							if (side != null)
//							{
//								boolean changed = false;
//								for (GenerableStructure str : c.structures)
//								{
//									str.draw(side);
//									changed = true;
//								}
//								if (changed)
//									side.need_render.set(true);
//								for (GenerableStructure str : side.structures)
//								{
//									str.draw(c);
//								}
//							}
//						}
//					}
//				}
//				// c.data[2][5][0] = 0;
//				// Cave evaluation
//				/*
//				 * for(Cave cave : caves) { int[] coords1 = cave.coords[0]; int
//				 * n = 1; while(n < cave.coords.length) { int[] coords2 =
//				 * cave.coords[n]; if(cave.care[n]) {
//				 * //System.out.println("c1:"+coords1[0]+"c2"+coords2[0]);
//				 * Vector3f vec = new
//				 * Vector3f(coords1[0]-coords2[0],coords1[1]-coords2
//				 * [1],coords1[2]-coords2[2]); float distance = vec.length();
//				 * vec.normalize(); float xpos = coords1[0]; float ypos =
//				 * coords1[1]; float zpos = coords1[2]; //System.out.println(
//				 * "lvl1 "+distance); if(nearChunk(cx,cz,coords1) &&
//				 * nearChunk(cx,cz,coords2)) { for(int g = 0; g <
//				 * (int)Math.ceil(distance); g+=1) { xpos+=vec.x; ypos+=vec.y;
//				 * zpos+=vec.z; int vx = (int) xpos; int vy = (int) ypos; int vz
//				 * = (int) zpos; int circleRadius = 2; for(int m =
//				 * vx-circleRadius; m < vx+circleRadius; m++) for(int r =
//				 * vy-circleRadius; r < vy+circleRadius; r++) for(int w =
//				 * vz-circleRadius; w < vz+circleRadius; w++) {
//				 * if(((m-vx)*(m-vx)+(r-vy)*(r-vy)+(w-vz)*(w-vz)) < 8 &&
//				 * areCoordsInsideChunk(cx,cz,new int[]{m,r,w})) { if(r > 0 && r
//				 * < 127) c.data[m%32][r][w%32] = VoxelFormat.format(0, 0, 0,
//				 * 0); //System.out.println("lvl3"); } } } }
//				 * 
//				 * } n++; coords1 = coords2; } }
//				 */
//				// end of caves
//			}
		return c;
	}

	/*
	 * private boolean nearChunk(int cx, int cz, int[] coords) { if(coords[0]+8
	 * >= cx*32 && coords[0]-8 < cx*32+32) if(coords[2]+8 >= cz*32 &&
	 * coords[2]-8 < cz*32+32) return true; return false; }
	 * 
	 * private boolean areCoordsInsideChunk(int cx, int cz, int[] coords) {
	 * if(coords[0] >= cx*32 && coords[0] < cx*32+32) if(coords[2] >= cz*32 &&
	 * coords[2] < cz*32+32) return true; return false; }
	 * 
	 * private void tryDecorating(int i, int j) { //System.out.println(
	 * "trying chunk decoration at : " +i+":"+j+"::"+world.isChunkLoaded2(i,j));
	 * if(world.isChunkLoaded(i,j)) { //System.out.println("!= -1 EXPLICIT"
	 * +world.isChunkLoaded2(i,j)); //System .out.println(
	 * "is unloaded (isCL2 == -1 ?) : "+(world.isChunkLoaded2(i,j) == -1));
	 * tryDecorating(world.getChunk(i, j, false)); } }
	 * 
	 * private void tryDecorating(Chunk c) { int empty = 0; int grass = 2;
	 * if(!c.hasBeenDecorated) { if(world.isChunkLoaded(c.chunkX+1,c.chunkZ) &&
	 * world.isChunkLoaded(c.chunkX-1,c.chunkZ) &&
	 * world.isChunkLoaded(c.chunkX,c.chunkZ+1) &&
	 * world.isChunkLoaded(c.chunkX,c.chunkZ-1) &&
	 * world.isChunkLoaded(c.chunkX+1,c.chunkZ+1) &&
	 * world.isChunkLoaded(c.chunkX+1,c.chunkZ-1) &&
	 * world.isChunkLoaded(c.chunkX-1,c.chunkZ+1) &&
	 * world.isChunkLoaded(c.chunkX-1,c.chunkZ-1)) { //System.out.println(
	 * "try cd3gs");
	 * 
	 * int sic = world.getSizeInChunks()*32; //Tree generation int cx =
	 * c.chunkX; int cz = c.chunkZ; int amountOfTrees = 2+(int)
	 * Math.abs((ssng.looped_noise((cx*32)/512f,(cz*32)/512f,sic/512f))*10);
	 * for(int i = 0; i < amountOfTrees; i++) { //Trees coords int xcoord =
	 * (int)(srnt.getRandomChanceForChunkPlusModifier(cx,cz,48+i)/1000f*32)%32;
	 * int zcoord =
	 * (int)(srnt.getRandomChanceForChunkPlusModifier(cx,cz,27+i)/1000f*32)%32;
	 * //Find air int ycoord = 0;
	 * while(VoxelFormat.id(c.data[xcoord][ycoord][zcoord]) != empty && ycoord <
	 * 127) { ycoord++; } //Is possible ? if(ycoord > 48 && ycoord < 115 &&
	 * VoxelFormat.id(c.data[xcoord][ycoord-1][zcoord]) == grass) { //Build the
	 * tree ! int treeHeight = 7+(int)randomLOL(c.chunkX,c.chunkZ,3)*2; for(int
	 * a = treeHeight-4; a < treeHeight; a++) { for(int b = -2; b <= 2; b++)
	 * for(int d = -2; d <= 2; d++)
	 * world.setDataAt(c.chunkX*32+xcoord+b,ycoord+a
	 * ,c.chunkZ*32+zcoord+d,VoxelFormat.format(5, 0, 0, 0)); } for(int b = -1;
	 * b <= 1; b++) for(int d = -1; d <= 1; d++)
	 * world.setDataAt(c.chunkX*32+xcoord
	 * +b,ycoord+treeHeight,c.chunkZ*32+zcoord+d,VoxelFormat.format(5, 0, 0,
	 * 0)); for(int a = 0; a < 7; a++) { c.data[xcoord][ycoord+a][zcoord] =
	 * VoxelFormat.format(4, 0, 0, 0); } } } c.hasBeenDecorated = true;
	 * c.needRender = true; } } }
	 */

}
