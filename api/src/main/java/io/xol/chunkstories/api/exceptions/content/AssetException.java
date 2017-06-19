package io.xol.chunkstories.api.exceptions.content;

import io.xol.chunkstories.api.mods.Asset;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class AssetException extends Exception {

	private static final long serialVersionUID = -1208547268257208116L;

	public Asset getAsset() {
		return asset;
	}

	public AssetException(Asset asset) {
		super();
		this.asset = asset;
	}

	private final Asset asset;
}
