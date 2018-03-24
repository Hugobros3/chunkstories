//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content;

import java.io.File;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.mesh.ClientMeshStore;
import io.xol.chunkstories.renderer.shaders.ShadersStore;
import io.xol.chunkstories.sound.SoundsLibrary;

public class ClientGameContent extends GameContentStore implements ClientContent
{
	private final ClientInterface client;
	
	private final ClientMeshStore meshes;
	private final TexturesLibrary textures;
	private final ShadersStore shaders;
	
	public ClientGameContent(ClientInterface client, File coreContentLocation, String modsStringArgument)
	{
		super(client, coreContentLocation, modsStringArgument);
		this.client = client;
		
		this.meshes = new ClientMeshStore(this, super.meshes);
		
		this.textures = new ClientTexturesLibrary(this);
		this.shaders = new ShadersStore(this);
	}

	@Override
	public void reload()
	{
		super.reload();
		
		//TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		
		this.meshes.reload();
		this.shaders.reloadAll();
		this.textures.reloadAll();
	}

	public ClientMeshStore meshes()
	{
		return meshes;
	}

	@Override
	public TexturesLibrary textures() {
		return textures;
	}

	public FontRenderer fonts() {
		return Client.getInstance()
				.getGameWindow()
				.getRenderingInterface().
				getFontRenderer();
	}

	@Override
	public ClientInterface getContext() {
		return client;
	}

	@Override
	public ShadersStore shaders() {
		return shaders;
	}
}
