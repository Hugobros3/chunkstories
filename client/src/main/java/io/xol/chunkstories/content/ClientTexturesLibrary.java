package io.xol.chunkstories.content;

import org.slf4j.Logger;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;
import io.xol.engine.graphics.textures.TextureGL;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

//TODO merge this & TexturesHandler
public class ClientTexturesLibrary implements TexturesLibrary {

	private final ClientGameContent clientGameContent;
	
	public ClientTexturesLibrary(ClientGameContent clientGameContent) {
		this.clientGameContent = clientGameContent;
	}

	@Override
	public Texture2D nullTexture() {
		return TexturesHandler.nullTexture();
	}

	@Override
	public Texture2D getTexture(String assetName) {
		return TexturesHandler.getTexture(assetName);
	}

	@Override
	public Texture2D newTexture2D(TextureFormat type, int width, int height) {
		return new Texture2DRenderTargetGL(type, width, height);
	}

	@Override
	public Cubemap getCubemap(String cubemapName) {
		return TexturesHandler.getCubemap(cubemapName);
	}

	@Override
	public ClientContent parent() {
		return clientGameContent;
	}

	@Override
	public void reloadAll() {
		TexturesHandler.reloadAll();
	}

	@Override
	public Logger logger() {
		return TextureGL.logger();
	}

}
