package io.xol.chunkstories.materials;

import io.xol.chunkstories.api.material.Material;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MaterialImplementation extends GenericNamedConfigurable implements Material
{	
	public MaterialImplementation(String name)
	{
		super(name);
		
		this.setProperty("sounds", "sounds/materials/<name>/");
		this.setProperty("walkingSounds", "sounds/footsteps/generic[1-3].ogg");
		this.setProperty("runningSounds", "<walkingSounds>");
		this.setProperty("jumpingSounds", "sounds/footsteps/jump.ogg");
		this.setProperty("landingSounds", "sounds/footsteps/land.ogg");
	}
}
