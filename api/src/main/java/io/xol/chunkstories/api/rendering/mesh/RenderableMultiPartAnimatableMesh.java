package io.xol.chunkstories.api.rendering.mesh;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderableMultiPartAnimatableMesh extends RenderableMultiPartMesh {

	/**
	 * Render only the meshes part selected and animates them using the Skeleton animation and animation time provided.
	 * The animation effect works by modifying the objectMatrix to account for each bone deformation assigned to each part.
	 * This works assuming each part of the renderable mesh is associated to one and only bone, of the same name.
	 * @throws RenderingException 
	 */
	public void render(RenderingInterface renderingInterface, SkeletonAnimator skeleton, double animationTime, String... parts ) throws RenderingException;
	
	/** Over(Under?)loaded version of the above function, using all parts of the mesh. */
	public void render(RenderingInterface renderingInterface, SkeletonAnimator skeleton, double animationTime ) throws RenderingException;
}
