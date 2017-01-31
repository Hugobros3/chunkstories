package io.xol.chunkstories.content;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientGameContent extends GameContentStore
{

	public ClientGameContent(ClientInterface client, String modsStringArgument)
	{
		super(client, modsStringArgument);
		
		String lang = Client.clientConfig.getProp("language", "undefined");
		if(!lang.equals("undefined"))
			super.localization().loadTranslation(lang);
	}

	@Override
	public void reload()
	{
		super.reload();
		
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		ModelLibrary.reloadAllModels();
		ShadersLibrary.reloadAllShaders();
	}

}
