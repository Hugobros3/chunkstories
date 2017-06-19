package io.xol.chunkstories.api.rendering.mesh;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderableMultiPartMesh extends RenderableMesh {
	
	/**
	 * Render only the meshes part selected
	 * @throws RenderingException 
	 */
	public void render(RenderingInterface renderingInterface, String... parts ) throws RenderingException;
	
	/** Lists the different parts this mesh is comprised of */
	public Iterable<String> allParts();
}
