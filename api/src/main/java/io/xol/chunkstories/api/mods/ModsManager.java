package io.xol.chunkstories.api.mods;

import java.util.Collection;
import java.util.Iterator;

import io.xol.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The ModsManager is responsible for loading mods and maintaining a global filesystem of assets
 */
public interface ModsManager
{	
	public void setEnabledMods(String... modsEnabled);

	public void loadEnabledMods() throws NotAllModsLoadedException;

	public Iterator<AssetHierarchy> getAllUniqueEntries();

	public Iterator<Asset> getAllUniqueFilesLocations();

	public Asset getAsset(String assetName);

	public AssetHierarchy getAssetInstances(String assetName);

	public Iterator<Asset> getAllAssetsByExtension(String extension);

	public Iterator<Asset> getAllAssetsByPrefix(String prefix);

	public Class<?> getClassByName(String className);

	public String[] getEnabledModsString();

	public Collection<Mod> getCurrentlyLoadedMods();
}