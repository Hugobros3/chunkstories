package io.xol.chunkstories.api.exceptions.content;

import io.xol.chunkstories.api.mods.Asset;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MeshLoadException extends AssetException {

	private static final long serialVersionUID = 5322485540396142553L;

	public MeshLoadException(Asset asset) {
		super(asset);
	}

	@Override
	public String getMessage() {
		return "Mesh from asset "+getAsset()+" failed to load.";
	}

	
}
