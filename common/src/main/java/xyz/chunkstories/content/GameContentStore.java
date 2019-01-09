//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.animation.BVHLibrary;
import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.content.mods.ModsManager;
import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException;
import xyz.chunkstories.content.mods.ModsManagerImplementation;
import xyz.chunkstories.content.mods.ModsManagerImplementation.NonExistentCoreContent;
import xyz.chunkstories.entity.EntityDefinitionsStore;
import xyz.chunkstories.item.ItemDefinitionsStore;
import xyz.chunkstories.localization.LocalizationManagerImplementation;
import xyz.chunkstories.mesh.MeshStore;
import xyz.chunkstories.net.PacketsStore;
import xyz.chunkstories.particle.ParticlesTypesStore;
import xyz.chunkstories.voxel.VoxelsStore;
import xyz.chunkstories.world.generator.WorldGeneratorsStore;

public class GameContentStore implements Content {
	private final GameContext context;
	private final ModsManager modsManager;

	private final ItemDefinitionsStore items;
	private final VoxelsStore voxels;
	private final EntityDefinitionsStore entities;
	private final PacketsStore packets;
	private final ParticlesTypesStore particles;
	private final WorldGeneratorsStore generators;

	private final BVHLibrary bvhLibrary;

	protected final MeshStore meshes;

	private final LocalizationManagerImplementation localizationManager;
	private final static Logger contentLogger = LoggerFactory.getLogger("content");

	public GameContentStore(GameContext context, File coreContentLocation, String enabledModsLaunchArguments) {
		this.context = context;
		try {
			this.modsManager = new ModsManagerImplementation(coreContentLocation, enabledModsLaunchArguments);
		} catch (NonExistentCoreContent e) {
			logger().error("Could not find core content at the location: " + coreContentLocation.getAbsolutePath());
			throw new RuntimeException("Throwing a RuntimeException to make the process crash and burn");
		}

		items = new ItemDefinitionsStore(this);
		voxels = new VoxelsStore(this);
		entities = new EntityDefinitionsStore(this);
		packets = new PacketsStore(this);
		particles = new ParticlesTypesStore(this);
		generators = new WorldGeneratorsStore(this);

		bvhLibrary = new BVHLibrary(this);

		meshes = new MeshStore(this);

		localizationManager = new LocalizationManagerImplementation(this, "en");
	}

	public void reload() {
		try {
			modsManager.loadEnabledMods();
		} catch (NotAllModsLoadedException e) {
			e.printStackTrace();
		}

		items.reload();
		voxels.reload();
		entities.reload();
		packets.reload();
		particles.reload();
		generators.reload();

		bvhLibrary.reloadAll();

		meshes.reloadAll();

		localizationManager.reload();
	}

	@Override
	public VoxelsStore voxels() {
		return voxels;
	}

	@Override
	public ItemDefinitionsStore items() {
		return items;
	}

	@Override
	public EntityDefinitionsStore entities() {
		return entities;
	}

	@Override
	public ParticlesTypesStore particles() {
		return particles;
	}

	@Override
	public PacketsStore packets() {
		return packets;
	}

	public GameContext getContext() {
		return context;
	}

	@Override
	public ModsManager modsManager() {
		return modsManager;
	}

	@Override
	public Asset getAsset(String assetName) {
		return modsManager.getAsset(assetName);
	}

	@Override
	public WorldGeneratorsStore generators() {
		return generators;
	}

	@Override
	public BVHLibrary getAnimationsLibrary() {
		return bvhLibrary;
	}

	@Override
	public LocalizationManager localization() {
		return localizationManager;
	}

	@Override
	public MeshStore meshes() {
		return meshes;
	}

	@Override
	public Logger logger() {
		return contentLogger;
	}
}
