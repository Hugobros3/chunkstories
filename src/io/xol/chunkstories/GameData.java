package io.xol.chunkstories;

import java.io.File;

import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.item.ItemsList;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.animation.BVHLibrary;
import io.xol.engine.sound.library.SoundsLibrary;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GameData
{
	public static void reload()
	{
		VoxelTextures.buildTextureAtlas();
		VoxelModels.resetAndLoadModels();
		VoxelTypes.loadVoxelTypes();
		EntitiesList.reload();
		ItemsList.reload();
	}
	
	public static void reloadClientContent()
	{
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		ModelLibrary.reloadAllModels();
		BVHLibrary.reloadAllAnimations();
	}

	public static File getTextureFileLocation(String textureName)
	{
		//TODO check mod folders
		if(textureName.endsWith(".png"))
			textureName = textureName.substring(0, textureName.length()-4);
		
		File checkTexturesFolder = new File("./res/textures/"+textureName+".png");
		if(checkTexturesFolder.exists())
			return checkTexturesFolder;
		File checkResFolder = new File("./res/"+textureName+".png");
		if(checkResFolder.exists())
			return checkResFolder;
		File checkRootFolder = new File("./"+textureName+".png");
		if(checkRootFolder.exists())
			return checkRootFolder;
		File checkGameFolder = new File(GameDirectory.getGameFolderPath()+"/"+textureName+".png");
		System.out.println(checkGameFolder);
		if(checkGameFolder.exists())
			return checkGameFolder;
		return null;
	}
}
