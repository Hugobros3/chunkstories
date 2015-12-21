package io.xol.chunkstories;

import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.item.ItemsList;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.engine.base.TexturesHandler;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.animation.BVHLibrary;

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
		ModelLibrary.reloadAllModels();
		BVHLibrary.reloadAllAnimations();
		TexturesHandler.mipmap("res/textures/tiles_merged_diffuse.png");
	}
}
