package io.xol.chunkstories.renderer.terrain;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer.FarTerrainRenderer;

/** Idea: stop with the idea of building meshes on the CPU. Just use pre-computed grids or a geometry shader and a big array texture with all the summaries
 * to draw this shit insanely fast.
 */
public class FarTerrainNoMeshRenderer implements FarTerrainRenderer {

	@Override
	public void markFarTerrainMeshDirty() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderTerrain(RenderingInterface renderer, ReadyVoxelMeshesMask mask) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void destroy() {
		
	}
}
