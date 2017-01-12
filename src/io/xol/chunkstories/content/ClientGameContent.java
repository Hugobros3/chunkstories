package io.xol.chunkstories.content;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.engine.animation.BVHLibrary;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientGameContent extends GameContent
{

	public ClientGameContent(ClientInterface client, String modsStringArgument)
	{
		super(client, modsStringArgument);
	}

	@Override
	public void reload()
	{
		super.reload();
		
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		ModelLibrary.reloadAllModels();
		BVHLibrary.reloadAllAnimations();
		ShadersLibrary.reloadAllShaders();
	}

}
