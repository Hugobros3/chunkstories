package io.xol.chunkstories.content;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.mesh.ClientMeshStore;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientGameContent extends GameContentStore implements ClientContent
{
	private final ClientMeshStore meshes;
	private final TexturesLibrary textures;
	
	public ClientGameContent(ClientInterface client, String modsStringArgument)
	{
		super(client, modsStringArgument);
		
		String lang = Client.clientConfig.getProp("language", "undefined");
		if(!lang.equals("undefined"))
			super.localization().loadTranslation(lang);
		
		this.meshes = new ClientMeshStore(this, super.meshes);
		
		this.textures = new ClientTexturesLibrary(this);
	}

	@Override
	public void reload()
	{
		super.reload();
		
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		//ModelLibrary.reloadAllModels();
		this.meshes.reload();
		
		ShadersLibrary.reloadAllShaders();
	}

	public ClientMeshStore meshes()
	{
		return meshes;
	}

	@Override
	public TexturesLibrary textures() {
		return textures;
	}
}
