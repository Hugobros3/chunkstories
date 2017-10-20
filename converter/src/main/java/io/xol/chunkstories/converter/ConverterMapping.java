package io.xol.chunkstories.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.VoxelChest;
import io.xol.chunkstories.core.voxel.VoxelDoor;
import io.xol.chunkstories.core.voxel.VoxelSign;

/** Maps minecraft ids to chunkstories's */
public class ConverterMapping {
	public final int MINECRAFT_IDS_CAP = 256;
	public final int MINECRAFT_METADATA_SIZE = 16;
	
	private Mapper[] mappers = new Mapper[MINECRAFT_IDS_CAP * MINECRAFT_METADATA_SIZE];
	
	public ConverterMapping(GameContext context, File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		 
		String line;
		while((line = reader.readLine()) != null) {
			if(!line.startsWith("#") && line.length() > 0) {
				String[] splitted = line.split(" ");
				if(splitted.length >= 2) {
					String mc = splitted[0];
					String cs = splitted[1];
					
					String special = splitted.length >= 3 ? splitted[2] : null;
					
					int minecraftID;
					byte minecraftMeta = -1;
					
					//Read the minecraft thingie
					if(mc.contains(":")) {
						minecraftID = Integer.parseInt(mc.split(":")[0]);
						minecraftMeta = Byte.parseByte(mc.split(":")[1]);
					} else {
						minecraftID = Integer.parseInt(mc);
					}
					
					//Read the cs part
					String chunkStoriesName = null;
					int chunkStoriesMeta = 0;
					if(cs.contains(":")) {
						chunkStoriesName = cs.split(":")[0];
						chunkStoriesMeta = Integer.parseInt(cs.split(":")[1]);
					} else {
						chunkStoriesName = cs;
					}
					
					Voxel voxel = context.getContent().voxels().getVoxelByName(chunkStoriesName);
					if(voxel == null) {
						System.out.println("Error: Voxel '"+chunkStoriesName+"' is nowhere to be found in the loaded content.");
						System.out.println("Skipping line : '"+line+"'.");
						continue;
					}
					
					Mapper mapper;
					if(special == null) {
						mapper = new TrivialMapper(voxel, chunkStoriesMeta);
					} else if(special.equals("keepmeta")) {
						mapper = new KeepMeta(voxel);
					} else if(special.equals("slab")) {
						mapper = new Slab(voxel);
					} else if(special.equals("door")) {
						mapper = new Door(voxel);
					} else if(special.equals("sign")) {
						mapper = new Sign(voxel);
					} else if(special.equals("chest")) {
						mapper = new Chest(voxel);
					} else {
						System.out.println("Error: mapper '"+special+"' was not recognised.");
						System.out.println("Skipping line : '"+line+"'.");
						continue;
					}
					
					//Fill the relevant cases
					if(minecraftMeta == -1) {
						for(int i = 0; i < 16; i++) {
							mappers[minecraftID * 16 + i] = mapper;
						}
					}
					else {
						mappers[minecraftID * 16 + minecraftMeta] = mapper;
					}
				}
			}
		}
		
		reader.close();
	}
	
	public Mapper getMapper(int minecraftID, byte minecraftMeta) {
		minecraftID &= 0xFF;
		minecraftMeta &= 0xF;
		
		return mappers[minecraftID * 16 + minecraftMeta];
	}
	
	abstract class Mapper {
		final Voxel voxel;
		final int voxelID;
		
		Mapper(Voxel voxel) {
			this.voxel = voxel;
			voxelID = voxel.getId();
		}
		
		abstract int output(int minecraftId, byte minecraftMeta);
	}
	
	class TrivialMapper extends Mapper {
		
		int baked;
		
		TrivialMapper(Voxel voxel, int meta) {
			super(voxel);
			baked = VoxelFormat.changeMeta(voxelID, meta);
		}
		
		int output(int mcId, byte mcMeta) {
			return baked;
		}
	}
	
	class KeepMeta extends Mapper {
		
		KeepMeta(Voxel voxel) {
			super(voxel);
		}

		@Override
		int output(int minecraftId, byte minecraftMeta) {
			return VoxelFormat.changeMeta(voxelID, minecraftMeta);
		}
	}
	
	class Slab extends Mapper {
		
		Slab(Voxel voxel) {
			super(voxel);
		}

		@Override
		int output(int minecraftId, byte minecraftMeta) {
			if(minecraftMeta >= 8)
				return VoxelFormat.changeMeta(voxelID, 1);
			else return voxelID;
		}
	}
	
	abstract class NonTrivialMapper extends Mapper {
		
		NonTrivialMapper(Voxel voxel) {
			super(voxel);
		}

		int output(int mcId, byte mcMeta) {
			throw new UnsupportedOperationException();
		}
		
		abstract void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData, MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion, int x, int y, int z);
	}
	
	class Door extends NonTrivialMapper {

		Door(Voxel voxel) {
			super(voxel);
		}

		@Override
		void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData, MinecraftRegion region,
				int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion, int x, int y, int z) {
			
			Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
			assert chunk != null;
			
			int upper = (minecraftMetaData & 0x8) >> 3;
			int open = (minecraftMetaData & 0x4) >> 2;
			
			//We only place the lower half of the door and the other half is created by the placing logic of chunk stories
			if (upper != 1)
			{
				int upperMeta = region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion).getBlockMeta(x, y + 1, z);
				
				int hingeSide = upperMeta & 0x01;
				
				int direction = minecraftMetaData & 0x3;
				int baked = VoxelFormat.format(voxelID, VoxelDoor.computeMeta(open == 1, hingeSide == 1, VoxelSides.getSideMcDoor(direction)), 0, 0);
				
				/*if (voxel instanceof VoxelDoor)
					try {
						//baked = ((VoxelDoor) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
					} catch (WorldException e) {
						
						e.printStackTrace();
						return;
					}
				else
					System.out.println("fuck you 666");*/
				
				csWorld.pokeSimpleSilently(csX, csY, csZ, baked);

			}
			else
				return;
			
		}
	}
	
	class Chest extends NonTrivialMapper {

		Chest(Voxel voxel) {
			super(voxel);
		}

		@Override
		void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
				MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion,
				int minecraftCuurrentChunkZinsideRegion, int x, int y, int z) {

			Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
			assert chunk != null;
			
			int baked = voxelID;
			
			if (voxel instanceof VoxelChest)
				try {
					baked = ((VoxelChest) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
				} catch (WorldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				System.out.println("fuck you 666");
			
			csWorld.pokeSimpleSilently(csX, csY, csZ, baked);
		}
		
	}
	
	class Sign extends NonTrivialMapper {

		Sign(Voxel voxel) {
			super(voxel);
		}

		@Override
		void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
				MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion,
				int minecraftCuurrentChunkZinsideRegion, int x, int y, int z) {

			Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
			assert chunk != null;
			
			int baked = voxelID;
			
			if (voxel instanceof VoxelSign) {
				
				if(!voxel.getName().endsWith("_post")) {
					if (minecraftMetaData == 2)
						minecraftMetaData = 8;
					else if (minecraftMetaData == 3)
						minecraftMetaData = 0;
					else if (minecraftMetaData == 4)
						minecraftMetaData = 4;
					else if (minecraftMetaData == 5)
						minecraftMetaData = 12;
				}
				
				baked = VoxelFormat.changeMeta(baked, minecraftMetaData);
				
				try {
					baked = ((VoxelSign) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
				} catch (WorldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				csWorld.pokeSimpleSilently(csX, csY, csZ, baked);
				
				//TODO Move Sign text getting here ?
				//EntityChest chestEntity = ((VoxelChest)voxel).getVoxelEntity(csWorld, csX, csY, csZ);
				//chestEntity.
			}
			else
				System.out.println("fuck you 666");
		}
		
	}
}
